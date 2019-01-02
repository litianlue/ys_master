/*
 * Synchronous I/O functions for libusb
 * Copyright © 2007-2008 Daniel Drake <dsd@gentoo.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include "config.h"
#include <errno.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "libusbi.h"

/**
 * @defgroup syncio Synchronous device I/O
 *
 * This page documents libusb's synchronous (blocking) API for USB device I/O.
 * This interface is easy to use but has some limitations. More advanced users
 * may wish to consider using the \ref asyncio "asynchronous I/O API" instead.
 */

static void LIBUSB_CALL sync_transfer_cb(struct libusb_transfer *transfer)
{
	int *completed = transfer->user_data;
	*completed = 1;
	usbi_dbg("actual_length=%d", transfer->actual_length);
	/* caller interprets result and frees transfer */
}

static void sync_transfer_wait_for_completion(struct libusb_transfer *transfer)
{
	int r, *completed = transfer->user_data;
	struct libusb_context *ctx = HANDLE_CTX(transfer->dev_handle);

	while (!*completed) {
		r = libusb_handle_events_completed(ctx, completed);
		if (UNLIKELY(r < 0)) {
			if (r == LIBUSB_ERROR_INTERRUPTED)
				continue;
			usbi_err(ctx, "libusb_handle_events failed: %s, cancelling transfer and retrying",
				 libusb_error_name(r));
			libusb_cancel_transfer(transfer);
			continue;
		}
	}
}

/** \ingroup syncio
 * Perform a USB control transfer.      //执行一个USB控制传输。
 *
 * The direction of the transfer is inferred from the bmRequestType field of
 * the setup packet.
 *
 * The wValue, wIndex and wLength fields values should be given in host-endian
 * byte order.
 *
  *   传输的方向是从bmrequesttype场推断
  *   设置数据包。
  *   的值，该值与wlength领域应在主机字节序端给
  *   字节顺序。

 * \param dev_handle        a handle for the device to communicate with     //一个用于与设备通信的手柄
 * \param bmRequestType     the request type field for the setup packet     //设置数据包的请求类型字段
 * \param bRequest          the request field for the setup packet          //设置数据包的请求域
 * \param wValue            the value field for the setup packet            //设置数据包的值字段
 * \param wIndex            the index field for the setup packet            //设置数据包的索引字段
 * \param data              a suitably-sized data buffer for either input or output         //一个适当大小的输入或输出数据缓冲器（取决于方向位在bmrequesttype）
 * (depending on direction bits within bmRequestType)
 * \param wLength           the length field for the setup packet. The data buffer should   //设置数据包的长度字段。数据缓冲区应该至少是这个大小。
 * be at least this size.
 * \param timeout           timeout (in millseconds) that this function should wait         //超时（在millseconds），这个函数应该在放弃之前由于没有响应被接收等。为无限超时，使用值0。
 * before giving up due to no response being received. For an unlimited
 * timeout, use value 0.
 *
 *
 * \returns on success, the number of bytes actually transferred                            //如果成功，返回 实际传输的字节数
 * \returns LIBUSB_ERROR_TIMEOUT        if the transfer timed out                           //如果传输超时 , 返回LIBUSB_ERROR_TIMEOUT
 * \returns LIBUSB_ERROR_PIPE           if the control request was not supported by the     //如果设备不支持控制请求 , 返回LIBUSB_ERROR_PIPE (错误的管道流) .
 * device
 * \returns LIBUSB_ERROR_NO_DEVICE      if the device has been disconnected                 //如果设备已断开 , 返回LIBUSB_ERROR_NO_DEVICE ;
 * \returns another                     LIBUSB_ERROR code on other failures                 //其他的错误 , 返回代码LIBUSB_ERROR ;
 */

int API_EXPORTED libusb_control_transfer(libusb_device_handle *dev_handle,
	uint8_t bmRequestType, uint8_t bRequest, uint16_t wValue, uint16_t wIndex,
	unsigned char *data, uint16_t wLength, unsigned int timeout)
{
	struct libusb_transfer *transfer = libusb_alloc_transfer(0);
	unsigned char *buffer;
	int completed = 0;
	int r;

	if (UNLIKELY(!transfer))
		return LIBUSB_ERROR_NO_MEM;

	buffer = (unsigned char*) malloc(LIBUSB_CONTROL_SETUP_SIZE + wLength);
	if (UNLIKELY(!buffer)) {
		libusb_free_transfer(transfer);
		return LIBUSB_ERROR_NO_MEM;
	}

	libusb_fill_control_setup(buffer, bmRequestType, bRequest, wValue, wIndex, wLength);
	if ((bmRequestType & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_OUT)
		memcpy(buffer + LIBUSB_CONTROL_SETUP_SIZE, data, wLength);

	libusb_fill_control_transfer(transfer, dev_handle, buffer,
		sync_transfer_cb, &completed, timeout);
	transfer->flags = LIBUSB_TRANSFER_FREE_BUFFER;
	r = libusb_submit_transfer(transfer);
	if (UNLIKELY(r < 0)) {
		libusb_free_transfer(transfer);
		return r;
	}

	sync_transfer_wait_for_completion(transfer);

	if ((bmRequestType & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN)
		memcpy(data, libusb_control_transfer_get_data(transfer),
			transfer->actual_length);

	switch (transfer->status) {
	case LIBUSB_TRANSFER_COMPLETED:
		r = transfer->actual_length;
		break;
	case LIBUSB_TRANSFER_TIMED_OUT:
		r = LIBUSB_ERROR_TIMEOUT;
		break;
	case LIBUSB_TRANSFER_STALL:
		r = LIBUSB_ERROR_PIPE;
		break;
	case LIBUSB_TRANSFER_NO_DEVICE:
		r = LIBUSB_ERROR_NO_DEVICE;
		break;
	case LIBUSB_TRANSFER_OVERFLOW:
		r = LIBUSB_ERROR_OVERFLOW;
		break;
	case LIBUSB_TRANSFER_ERROR:
	case LIBUSB_TRANSFER_CANCELLED:
		r = LIBUSB_ERROR_IO;
		break;
	default:
		usbi_warn(HANDLE_CTX(dev_handle),
			"unrecognised status code %d", transfer->status);
		r = LIBUSB_ERROR_OTHER;
	}

	libusb_free_transfer(transfer);
	return r;
}

static int do_sync_bulk_transfer(struct libusb_device_handle *dev_handle,
	unsigned char endpoint, unsigned char *buffer, int length,
	int *transferred, unsigned int timeout, unsigned char type)
{
	struct libusb_transfer *transfer = libusb_alloc_transfer(0);
	int completed = 0;
	int r;

	if (UNLIKELY(!transfer))
		return LIBUSB_ERROR_NO_MEM;

	libusb_fill_bulk_transfer(transfer, dev_handle, endpoint, buffer, length,
		sync_transfer_cb, &completed, timeout);
	transfer->type = type;

	r = libusb_submit_transfer(transfer);
	if (UNLIKELY(r < 0)) {
		libusb_free_transfer(transfer);
		return r;
	}

	sync_transfer_wait_for_completion(transfer);

	*transferred = transfer->actual_length;
	switch (transfer->status) {
	case LIBUSB_TRANSFER_COMPLETED:
		r = 0;
		break;
	case LIBUSB_TRANSFER_TIMED_OUT:
		r = LIBUSB_ERROR_TIMEOUT;
		break;
	case LIBUSB_TRANSFER_STALL:
		r = LIBUSB_ERROR_PIPE;
		break;
	case LIBUSB_TRANSFER_OVERFLOW:
		r = LIBUSB_ERROR_OVERFLOW;
		break;
	case LIBUSB_TRANSFER_NO_DEVICE:
		r = LIBUSB_ERROR_NO_DEVICE;
		break;
	case LIBUSB_TRANSFER_ERROR:
	case LIBUSB_TRANSFER_CANCELLED:
		r = LIBUSB_ERROR_IO;
		break;
	default:
		usbi_warn(HANDLE_CTX(dev_handle),
			"unrecognised status code %d", transfer->status);
		r = LIBUSB_ERROR_OTHER;
	}

	libusb_free_transfer(transfer);
	return r;
}

/** \ingroup syncio
 * Perform a USB bulk transfer. The direction of the transfer is inferred from
 * the direction bits of the endpoint address.
 *
 * For bulk reads, the <tt>length</tt> field indicates the maximum length of
 * data you are expecting to receive. If less data arrives than expected,
 * this function will return that data, so be sure to check the
 * <tt>transferred</tt> output parameter.
 *
 * You should also check the <tt>transferred</tt> parameter for bulk writes.
 * Not all of the data may have been written.
 *
 * Also check <tt>transferred</tt> when dealing with a timeout error code.
 * libusb may have to split your transfer into a number of chunks to satisfy
 * underlying O/S requirements, meaning that the timeout may expire after
 * the first few chunks have completed. libusb is careful not to lose any data
 * that may have been transferred; do not assume that timeout conditions
 * indicate a complete lack of I/O.
 *
 * \param dev_handle a handle for the device to communicate with
 * \param endpoint the address of a valid endpoint to communicate with
 * \param data a suitably-sized data buffer for either input or output
 * (depending on endpoint)
 * \param length for bulk writes, the number of bytes from data to be sent. for
 * bulk reads, the maximum number of bytes to receive into the data buffer.
 * \param transferred output location for the number of bytes actually
 * transferred.
 * \param timeout timeout (in millseconds) that this function should wait
 * before giving up due to no response being received. For an unlimited
 * timeout, use value 0.
 *
 * \returns 0 on success (and populates <tt>transferred</tt>)
 * \returns LIBUSB_ERROR_TIMEOUT if the transfer timed out (and populates
 * <tt>transferred</tt>)
 * \returns LIBUSB_ERROR_PIPE if the endpoint halted
 * \returns LIBUSB_ERROR_OVERFLOW if the device offered more data, see
 * \ref packetoverflow
 * \returns LIBUSB_ERROR_NO_DEVICE if the device has been disconnected
 * \returns another LIBUSB_ERROR code on other failures
 */
int API_EXPORTED libusb_bulk_transfer(struct libusb_device_handle *dev_handle,
	unsigned char endpoint, unsigned char *data, int length, int *transferred,
	unsigned int timeout)
{
	return do_sync_bulk_transfer(dev_handle, endpoint, data, length,
		transferred, timeout, LIBUSB_TRANSFER_TYPE_BULK);
}

/** \ingroup syncio
 * Perform a USB interrupt transfer. The direction of the transfer is inferred
 * from the direction bits of the endpoint address.
 *
 * For interrupt reads, the <tt>length</tt> field indicates the maximum length
 * of data you are expecting to receive. If less data arrives than expected,
 * this function will return that data, so be sure to check the
 * <tt>transferred</tt> output parameter.
 *
 * You should also check the <tt>transferred</tt> parameter for interrupt
 * writes. Not all of the data may have been written.
 *
 * Also check <tt>transferred</tt> when dealing with a timeout error code.
 * libusb may have to split your transfer into a number of chunks to satisfy
 * underlying O/S requirements, meaning that the timeout may expire after
 * the first few chunks have completed. libusb is careful not to lose any data
 * that may have been transferred; do not assume that timeout conditions
 * indicate a complete lack of I/O.
 *
 * The default endpoint bInterval value is used as the polling interval.
 *
 * \param dev_handle a handle for the device to communicate with
 * \param endpoint the address of a valid endpoint to communicate with
 * \param data a suitably-sized data buffer for either input or output
 * (depending on endpoint)
 * \param length for bulk writes, the number of bytes from data to be sent. for
 * bulk reads, the maximum number of bytes to receive into the data buffer.
 * \param transferred output location for the number of bytes actually
 * transferred.
 * \param timeout timeout (in millseconds) that this function should wait
 * before giving up due to no response being received. For an unlimited
 * timeout, use value 0.
 *
 * \returns 0 on success (and populates <tt>transferred</tt>)
 * \returns LIBUSB_ERROR_TIMEOUT if the transfer timed out
 * \returns LIBUSB_ERROR_PIPE if the endpoint halted
 * \returns LIBUSB_ERROR_OVERFLOW if the device offered more data, see
 * \ref packetoverflow
 * \returns LIBUSB_ERROR_NO_DEVICE if the device has been disconnected
 * \returns another LIBUSB_ERROR code on other error
 */
int API_EXPORTED libusb_interrupt_transfer(
	struct libusb_device_handle *dev_handle, unsigned char endpoint,
	unsigned char *data, int length, int *transferred, unsigned int timeout)
{
	return do_sync_bulk_transfer(dev_handle, endpoint, data, length,
		transferred, timeout, LIBUSB_TRANSFER_TYPE_INTERRUPT);
}
