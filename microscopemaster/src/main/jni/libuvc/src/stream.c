/*********************************************************************
 *********************************************************************/
/*********************************************************************
 * modified some function to avoid crash
 * Copyright (C) 2014-2015 Mr_Wen@yeespec All rights reserved.
 *********************************************************************/
/*********************************************************************
 * Software License Agreement (BSD License)
 *
 *  Copyright (C) 2010-2012 Ken Tossell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the author nor other contributors may be
 *     used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************/
/**
 * @defgroup streaming Streaming control functions
 * @brief Tools for creating, managing and consuming video streams
 */

#include <assert.h>		// XXX add assert for debugging
#include <unistd.h>
#include "libuvc/libuvc.h"
#include "libuvc/libuvc_internal.h"

uvc_frame_desc_t *uvc_find_frame_desc_stream(uvc_stream_handle_t *strmh,
		uint16_t format_id, uint16_t frame_id);
uvc_frame_desc_t *uvc_find_frame_desc(uvc_device_handle_t *devh,
		uint16_t format_id, uint16_t frame_id);
static void *_uvc_user_caller(void *arg);
static void _uvc_populate_frame(uvc_stream_handle_t *strmh);

struct format_table_entry {
	enum uvc_frame_format format;
	uint8_t abstract_fmt;
	uint8_t guid[16];
	int children_count;
	enum uvc_frame_format *children;
};

struct format_table_entry *_get_format_entry(enum uvc_frame_format format) {
#define ABS_FMT(_fmt, ...) \
    case _fmt: { \
    static enum uvc_frame_format _fmt##_children[] = __VA_ARGS__; \
    static struct format_table_entry _fmt##_entry = { \
      _fmt, 0, {}, ARRAYSIZE(_fmt##_children), _fmt##_children }; \
    return &_fmt##_entry; }

#define FMT(_fmt, ...) \
    case _fmt: { \
    static struct format_table_entry _fmt##_entry = { \
      _fmt, 0, __VA_ARGS__, 0, NULL }; \
    return &_fmt##_entry; }

	switch (format) {
	/* Define new formats here */
	ABS_FMT(UVC_FRAME_FORMAT_ANY,
		{UVC_FRAME_FORMAT_UNCOMPRESSED, UVC_FRAME_FORMAT_COMPRESSED})

	ABS_FMT(UVC_FRAME_FORMAT_UNCOMPRESSED,
		{UVC_FRAME_FORMAT_YUYV, UVC_FRAME_FORMAT_UYVY, UVC_FRAME_FORMAT_GRAY8})
	FMT(UVC_FRAME_FORMAT_YUYV,
		{'Y', 'U', 'Y', '2', 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71})
	FMT(UVC_FRAME_FORMAT_UYVY,
		{'U', 'Y', 'V', 'Y', 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71})
	FMT(UVC_FRAME_FORMAT_GRAY8,
		{'Y', '8', '0', '0', 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71})
    FMT(UVC_FRAME_FORMAT_BY8,
    	{'B', 'Y', '8', ' ', 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71})

	ABS_FMT(UVC_FRAME_FORMAT_COMPRESSED,
		{UVC_FRAME_FORMAT_MJPEG})
	FMT(UVC_FRAME_FORMAT_MJPEG,
		{'M', 'J', 'P', 'G'})

	default:
		return NULL;
	}

#undef ABS_FMT
#undef FMT
}

static uint8_t _uvc_frame_format_matches_guid(enum uvc_frame_format fmt,
		uint8_t guid[16]) {
	struct format_table_entry *format;
	int child_idx;

	format = _get_format_entry(fmt);
	if (UNLIKELY(!format))
		return 0;

	if (!format->abstract_fmt && !memcmp(guid, format->guid, 16))
		return 1;

	for (child_idx = 0; child_idx < format->children_count; child_idx++) {
		if (_uvc_frame_format_matches_guid(format->children[child_idx], guid))
			return 1;
	}

	return 0;
}

static enum uvc_frame_format uvc_frame_format_for_guid(uint8_t guid[16]) {

	struct format_table_entry *format;
	enum uvc_frame_format fmt;

	for (fmt = 0; fmt < UVC_FRAME_FORMAT_COUNT; ++fmt) {
		format = _get_format_entry(fmt);
		if (!format || format->abstract_fmt)
			continue;
		if (!memcmp(format->guid, guid, 16))
			return format->format;
	}

	return UVC_FRAME_FORMAT_UNKNOWN;
}

/** @internal
 * Run a streaming control query    //运行一个流控制查询
 * @param[in] devh UVC device
 * @param[in,out] ctrl Control block
 * @param[in] probe Whether this is a probe query or a commit query
 * @param[in] req Query type
 */
uvc_error_t uvc_query_stream_ctrl(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, uint8_t probe, enum uvc_req_code req) {
	uint8_t buf[48];	// XXX support UVC 1.1 & 1.5
	size_t len;
	uvc_error_t err;

	memset(buf, 0, sizeof(buf));	// bzero(buf, sizeof(buf));	// bzero is deprecated

	const uint16_t bcdUVC = devh->info->ctrl_if.bcdUVC;
	if (bcdUVC >= 0x0150)
		len = 48;
	else if (bcdUVC >= 0x0110)
		len = 34;
	else
		len = 26;
//	LOGI("bcdUVC:%x,req:0x%02x,probe:%d", bcdUVC, req, probe);
	/* prepare for a SET transfer 准备一个集合传输 */
	if (req == UVC_SET_CUR) {
		SHORT_TO_SW(ctrl->bmHint, buf);
		buf[2] = ctrl->bFormatIndex;
		buf[3] = ctrl->bFrameIndex;
		INT_TO_DW(ctrl->dwFrameInterval, buf + 4);
		SHORT_TO_SW(ctrl->wKeyFrameRate, buf + 8);
		SHORT_TO_SW(ctrl->wPFrameRate, buf + 10);
		SHORT_TO_SW(ctrl->wCompQuality, buf + 12);
		SHORT_TO_SW(ctrl->wCompWindowSize, buf + 14);
		SHORT_TO_SW(ctrl->wDelay, buf + 16);
		INT_TO_DW(ctrl->dwMaxVideoFrameSize, buf + 18);
		INT_TO_DW(ctrl->dwMaxPayloadTransferSize, buf + 22);

		if (len > 26) {	// len == 34
			// XXX add to support UVC 1.1
			INT_TO_DW(ctrl->dwClockFrequency, buf + 26);
			buf[30] = ctrl->bmFramingInfo;
			buf[31] = ctrl->bPreferedVersion;
			buf[32] = ctrl->bMinVersion;
			buf[33] = ctrl->bMaxVersion;
			if (len == 48) {
				// XXX add to support UVC1.5
				buf[34] = ctrl->bUsage;
				buf[35] = ctrl->bBitDepthLuma;
				buf[36] = ctrl->bmSettings;
				buf[37] = ctrl->bMaxNumberOfRefFramesPlus1;
				SHORT_TO_SW(ctrl->bmRateControlModes, buf + 38);
				LONG_TO_QW(ctrl->bmLayoutPerStream, buf + 40);
			}
		}
	}

	/* do the transfer 进行数据传输 */
	err = libusb_control_transfer(devh->usb_devh,
			req == UVC_SET_CUR ? 0x21 : 0xA1, req,
			probe ? (UVC_VS_PROBE_CONTROL << 8) : (UVC_VS_COMMIT_CONTROL << 8),
			ctrl->bInterfaceNumber, buf, len, 0);

	if (UNLIKELY(err <= 0)) {
		// when libusb_control_transfer returned error or transfer bytes was zero.  //当libusb_control_transfer返回错误或传输字节为零。
		if (!err) {
			UVC_DEBUG("libusb_control_transfer transfered zero length data");
			err = UVC_ERROR_OTHER;
		}
		return err;
	}
	if (err < len) {
#if !defined(__LP64__)
		LOGE("transfered bytes is smaller than data bytes:%d expected %d", err, len);
#else
		LOGE("transfered bytes is smaller than data bytes:%d expected %ld", err, len);
#endif
		return UVC_ERROR_OTHER;
	}
	/* now decode following a GET transfer */
	if (req != UVC_SET_CUR) {
		ctrl->bmHint = SW_TO_SHORT(buf);
		ctrl->bFormatIndex = buf[2];
		ctrl->bFrameIndex = buf[3];
		ctrl->dwFrameInterval = DW_TO_INT(buf + 4);
		ctrl->wKeyFrameRate = SW_TO_SHORT(buf + 8);
		ctrl->wPFrameRate = SW_TO_SHORT(buf + 10);
		ctrl->wCompQuality = SW_TO_SHORT(buf + 12);
		ctrl->wCompWindowSize = SW_TO_SHORT(buf + 14);
		ctrl->wDelay = SW_TO_SHORT(buf + 16);
		ctrl->dwMaxVideoFrameSize = DW_TO_INT(buf + 18);
		ctrl->dwMaxPayloadTransferSize = DW_TO_INT(buf + 22);

		if (len > 26) {	// len == 34
			// XXX add to support UVC 1.1
			ctrl->dwClockFrequency = DW_TO_INT(buf + 26);
			ctrl->bmFramingInfo = buf[30];
			ctrl->bPreferedVersion = buf[31];
			ctrl->bMinVersion = buf[32];
			ctrl->bMaxVersion = buf[33];
			if (len >= 48) {
				// XXX add to support UVC1.5
				ctrl->bUsage = buf[34];
				ctrl->bBitDepthLuma = buf[35];
				ctrl->bmSettings = buf[36];
				ctrl->bMaxNumberOfRefFramesPlus1 = buf[37];
				ctrl->bmRateControlModes = SW_TO_SHORT(buf + 38);
				ctrl->bmLayoutPerStream = QW_TO_LONG(buf + 40);
			}
		}

		/* fix up block for cameras that fail to set dwMax */
		if (!ctrl->dwMaxVideoFrameSize) {
			LOGW("fix up block for cameras that fail to set dwMax");
			uvc_frame_desc_t *frame_desc = uvc_find_frame_desc(devh,
					ctrl->bFormatIndex, ctrl->bFrameIndex);

			if (frame_desc) {
				ctrl->dwMaxVideoFrameSize = frame_desc->dwMaxVideoFrameBufferSize;
			}
		}
	}

	return UVC_SUCCESS;
}

/** @brief Reconfigure stream with a new stream format.
 * @ingroup streaming
 *
 * This may be executed whether or not the stream is running.
 *
 * @param[in] strmh Stream handle
 * @param[in] ctrl Control block, processed using {uvc_probe_stream_ctrl} or
 *             {uvc_get_stream_ctrl_format_size}
 */
uvc_error_t uvc_stream_ctrl(uvc_stream_handle_t *strmh, uvc_stream_ctrl_t *ctrl) {
	uvc_error_t ret;

	if (UNLIKELY(strmh->stream_if->bInterfaceNumber != ctrl->bInterfaceNumber))
		return UVC_ERROR_INVALID_PARAM;

	/* @todo Allow the stream to be modified without restarting the stream */
	if (UNLIKELY(strmh->running))
		return UVC_ERROR_BUSY;

	ret = uvc_query_stream_ctrl(strmh->devh, ctrl, 0, UVC_SET_CUR);	// commit query
	if (UNLIKELY(ret != UVC_SUCCESS))
		return ret;

	strmh->cur_ctrl = *ctrl;
	return UVC_SUCCESS;
}

/** @internal
 * @brief Find the descriptor for a specific frame configuration
 * @param stream_if Stream interface
 * @param format_id Index of format class descriptor
 * @param frame_id Index of frame descriptor
 */
static uvc_frame_desc_t *_uvc_find_frame_desc_stream_if(
		uvc_streaming_interface_t *stream_if, uint16_t format_id,
		uint16_t frame_id) {

	uvc_format_desc_t *format = NULL;
	uvc_frame_desc_t *frame = NULL;

	DL_FOREACH(stream_if->format_descs, format)
	{
		if (format->bFormatIndex == format_id) {
			DL_FOREACH(format->frame_descs, frame)
			{
				if (frame->bFrameIndex == frame_id)
					return frame;
			}
		}
	}

	return NULL ;
}

uvc_error_t uvc_get_frame_desc(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, uvc_frame_desc_t **desc) {

	*desc = uvc_find_frame_desc(devh, ctrl->bFormatIndex, ctrl->bFrameIndex);
	return *desc ? UVC_SUCCESS : UVC_ERROR_INVALID_PARAM;
}

uvc_frame_desc_t *uvc_find_frame_desc_stream(uvc_stream_handle_t *strmh,
		uint16_t format_id, uint16_t frame_id) {
	return _uvc_find_frame_desc_stream_if(strmh->stream_if, format_id, frame_id);
}

/** @internal
 * @brief Find the descriptor for a specific frame configuration
 * @param devh UVC device
 * @param format_id Index of format class descriptor
 * @param frame_id Index of frame descriptor
 */
uvc_frame_desc_t *uvc_find_frame_desc(uvc_device_handle_t *devh,
		uint16_t format_id, uint16_t frame_id) {

	uvc_streaming_interface_t *stream_if;
	uvc_frame_desc_t *frame;

	DL_FOREACH(devh->info->stream_ifs, stream_if)
	{
		frame = _uvc_find_frame_desc_stream_if(stream_if, format_id, frame_id);
		if (frame)
			return frame;
	}

	return NULL;
}

static void _uvc_print_streaming_interface_one(uvc_streaming_interface_t *stream_if) {
//	struct uvc_device_info *parent;
//	struct uvc_streaming_interface *prev, *next;
	MARK("bInterfaceNumber:%d", stream_if->bInterfaceNumber);
	uvc_print_format_desc_one(stream_if->format_descs, NULL);
	MARK("bEndpointAddress:%d", stream_if->bEndpointAddress);
	MARK("bTerminalLink:%d", stream_if->bTerminalLink);
}

static uvc_error_t _prepare_stream_ctrl(uvc_device_handle_t *devh, uvc_stream_ctrl_t *ctrl) {
	// XXX some camera may need to call uvc_query_stream_ctrl with UVC_GET_CUR/UVC_GET_MAX/UVC_GET_MIN
	// before negotiation otherwise stream stall. added by Mr_Wen
	uvc_error_t result = uvc_query_stream_ctrl(devh, ctrl, 1, UVC_GET_CUR);	// probe query
	if (LIKELY(!result)) {
		result = uvc_query_stream_ctrl(devh, ctrl, 1, UVC_GET_MIN);			// probe query
		if (LIKELY(!result)) {
			result = uvc_query_stream_ctrl(devh, ctrl, 1, UVC_GET_MAX);		// probe query
			if (UNLIKELY(result))
				LOGE("uvc_query_stream_ctrl:UVC_GET_MAX:err=%d", result);	// XXX 最大値の方を後で取得しないとだめ
		} else {
			LOGE("uvc_query_stream_ctrl:UVC_GET_MIN:err=%d", result);
		}
	} else {
		LOGE("uvc_query_stream_ctrl:UVC_GET_CUR:err=%d", result);
	}
#if 0
	if (UNLIKELY(result)) {
		enum uvc_error_code_control error_code;
		uvc_get_error_code(devh, &error_code, UVC_GET_CUR);
		LOGE("uvc_query_stream_ctrl:ret=%d,err_code=%d", result, error_code);
		uvc_print_format_desc(devh->info->stream_ifs->format_descs, NULL);
	}
#endif
	return result;
}

static uvc_error_t _uvc_get_stream_ctrl_format(uvc_device_handle_t *devh,
	uvc_streaming_interface_t *stream_if, uvc_stream_ctrl_t *ctrl, uvc_format_desc_t *format,
	const int width, const int height,
	const int min_fps, const int max_fps) {

	ENTER();

	int i;
	uvc_frame_desc_t *frame;

	ctrl->bInterfaceNumber = stream_if->bInterfaceNumber;
	uvc_error_t result = uvc_claim_if(devh, ctrl->bInterfaceNumber);
	if (UNLIKELY(result)) {
		LOGE("uvc_claim_if:err=%d", result);
		goto fail;
	}
	for (i = 0; i < 2; i++) {
		result = _prepare_stream_ctrl(devh, ctrl);
	}
	if (UNLIKELY(result)) {
		LOGE("_prepare_stream_ctrl:err=%d", result);
		goto fail;
	}
#if 0
	// XXX add check ctrl values
	uint64_t bmaControl = stream_if->bmaControls[format->bFormatIndex - 1];
	if (bmaControl & 0x001) {	// wKeyFrameRate
		if (UNLIKELY(!ctrl->wKeyFrameRate)) {
			LOGE("wKeyFrameRate should be set");
			RETURN(UVC_ERROR_INVALID_MODE, uvc_error_t);
		}
	}
	if (bmaControl & 0x002) {	// wPFrameRate
		if (UNLIKELY(!ctrl->wPFrameRate)) {
			LOGE("wPFrameRate should be set");
			RETURN(UVC_ERROR_INVALID_MODE, uvc_error_t);
		}
	}
	if (bmaControl & 0x004) {	// wCompQuality
		if (UNLIKELY(!ctrl->wCompQuality)) {
			LOGE("wCompQuality should be set");
			RETURN(UVC_ERROR_INVALID_MODE, uvc_error_t);
		}
	}
	if (bmaControl & 0x008) {	// wCompWindowSize
		if (UNLIKELY(!ctrl->wCompWindowSize)) {
			LOGE("wCompWindowSize should be set");
			RETURN(UVC_ERROR_INVALID_MODE, uvc_error_t);
		}
	}
#endif
	DL_FOREACH(format->frame_descs, frame)
	{
		if (frame->wWidth != width || frame->wHeight != height)
			continue;

		uint32_t *interval;

		if (frame->intervals) {
			for (interval = frame->intervals; *interval; ++interval) {
				uint32_t it = 10000000 / *interval;
				LOGV("it:%d", it);
				if ((it >= (uint32_t) min_fps) && (it <= (uint32_t) max_fps)) {
					ctrl->bmHint = (1 << 0); /* don't negotiate interval */
					ctrl->bFormatIndex = format->bFormatIndex;
					ctrl->bFrameIndex = frame->bFrameIndex;
					ctrl->dwFrameInterval = *interval;

					goto found;
				}
			}
		} else {
			int32_t fps;
			for (fps = max_fps; fps >= min_fps; fps--) {
				uint32_t interval_100ns = 10000000 / fps;
				uint32_t interval_offset = interval_100ns - frame->dwMinFrameInterval;
				LOGV("fps:%d", fps);
				if (interval_100ns >= frame->dwMinFrameInterval
					&& interval_100ns <= frame->dwMaxFrameInterval
					&& !(interval_offset
						&& (interval_offset % frame->dwFrameIntervalStep) ) ) {
					ctrl->bmHint = (1 << 0); /* don't negotiate interval */
					ctrl->bFormatIndex = format->bFormatIndex;
					ctrl->bFrameIndex = frame->bFrameIndex;
					ctrl->dwFrameInterval = interval_100ns;

					goto found;
				}
			}
		}
	}
	result = UVC_ERROR_INVALID_MODE;
fail:
	uvc_release_if(devh, ctrl->bInterfaceNumber);
	RETURN(result, uvc_error_t);

found:
	RETURN(UVC_SUCCESS, uvc_error_t);
}

/** Get a negotiated streaming control block for some common parameters.
 * @ingroup streaming
 *
 * @param[in] devh Device handle
 * @param[in,out] ctrl Control block
 * @param[in] cf Type of streaming format
 * @param[in] width Desired frame width
 * @param[in] height Desired frame height
 * @param[in] fps Frame rate, frames per second
 */
uvc_error_t uvc_get_stream_ctrl_format_size(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, enum uvc_frame_format cf, int width, int height, int fps) {

	return uvc_get_stream_ctrl_format_size_fps(devh, ctrl, cf, width, height, fps, fps);
}

/** Get a negotiated streaming control block for some common parameters.
 * @ingroup streaming
 *
 * @param[in] devh Device handle
 * @param[in,out] ctrl Control block
 * @param[in] cf Type of streaming format
 * @param[in] width Desired frame width
 * @param[in] height Desired frame height
 * @param[in] min_fps Frame rate, minimum frames per second, this value is included
 * @param[in] max_fps Frame rate, maximum frames per second, this value is included
 */
uvc_error_t uvc_get_stream_ctrl_format_size_fps(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, enum uvc_frame_format cf, int width,
		int height, int min_fps, int max_fps) {

	ENTER();

	uvc_streaming_interface_t *stream_if;
	uvc_error_t result;

	memset(ctrl, 0, sizeof(*ctrl));	// XXX add
	/* find a matching frame descriptor and interval */
	uvc_format_desc_t *format;
	DL_FOREACH(devh->info->stream_ifs, stream_if)
	{
		DL_FOREACH(stream_if->format_descs, format)
		{
			if (!_uvc_frame_format_matches_guid(cf, format->guidFormat))
				continue;

			result = _uvc_get_stream_ctrl_format(devh, stream_if, ctrl, format, width, height, min_fps, max_fps);
			if (!result) {	// UVC_SUCCESS
				goto found;
			}
		}
	}

	RETURN(UVC_ERROR_INVALID_MODE, uvc_error_t);

found:
	RETURN(uvc_probe_stream_ctrl(devh, ctrl), uvc_error_t);
}

/** @internal
 * Negotiate streaming parameters with the device
 *
 * @param[in] devh UVC device
 * @param[in,out] ctrl Control block
 */
uvc_error_t uvc_probe_stream_ctrl(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl) {
	uvc_error_t err;

	err = uvc_claim_if(devh, ctrl->bInterfaceNumber);
	if (UNLIKELY(err)) {
		LOGE("uvc_claim_if:err=%d", err);
		return err;
	}

	err = uvc_query_stream_ctrl(devh, ctrl, 1, UVC_SET_CUR);	// probe query
	if (UNLIKELY(err)) {
		LOGE("uvc_query_stream_ctrl(UVC_SET_CUR):err=%d", err);
		return err;
	}

	err = uvc_query_stream_ctrl(devh, ctrl, 1, UVC_GET_CUR);	// probe query ここでエラーが返ってくる
	if (UNLIKELY(err)) {
		LOGE("uvc_query_stream_ctrl(UVC_GET_CUR):err=%d", err);
		return err;
	}

	return UVC_SUCCESS;
}

/** @internal
 * @brief Swap the working buffer with the presented buffer and notify consumers    //用缓冲区交换工作缓冲区并通知用户
 */
static void _uvc_swap_buffers(uvc_stream_handle_t *strmh) {     //用缓冲区交换工作缓冲区并通知用户
	uint8_t *tmp_buf;

	pthread_mutex_lock(&strmh->cb_mutex);
	{
		/* swap the buffers
		//交换缓冲区
		互换holdbuf和outbuf缓冲的视频帧数据 ;
		*/
		tmp_buf = strmh->holdbuf;
		strmh->hold_bfh_err = strmh->bfh_err;	// XXX
		strmh->hold_bytes = strmh->got_bytes;
		strmh->holdbuf = strmh->outbuf;
		strmh->outbuf = tmp_buf;

		strmh->hold_last_scr = strmh->last_scr;
		strmh->hold_pts = strmh->pts;
		strmh->hold_seq = strmh->seq;

		pthread_cond_broadcast(&strmh->cb_cond);
	}
	pthread_mutex_unlock(&strmh->cb_mutex);

	strmh->seq++;
	strmh->got_bytes = 0;
	strmh->last_scr = 0;
	strmh->pts = 0;
	strmh->bfh_err = 0;	// XXX
}

static void _uvc_delete_transfer(struct libusb_transfer *transfer) {
	ENTER();

//	MARK("");
	uvc_stream_handle_t *strmh = transfer->user_data;
	if (UNLIKELY(!strmh)) EXIT();		// XXX
	int i;

	pthread_mutex_lock(&strmh->cb_mutex);	// XXX crash while calling uvc_stop_streaming
	{
		// Mark transfer as deleted.
		for (i = 0; i < LIBUVC_NUM_TRANSFER_BUFS; i++) {
			if (strmh->transfers[i] == transfer) {
				libusb_cancel_transfer(strmh->transfers[i]);	// XXX 20141112追加
				UVC_DEBUG("Freeing transfer %d (%p)", i, transfer);
				free(transfer->buffer);
				libusb_free_transfer(transfer);
				strmh->transfers[i] = NULL;
				break;
			}
		}
		if (UNLIKELY(i == LIBUVC_NUM_TRANSFER_BUFS)) {
			UVC_DEBUG("transfer %p not found; not freeing!", transfer);
		}

		pthread_cond_broadcast(&strmh->cb_cond);
	}
	pthread_mutex_unlock(&strmh->cb_mutex);
	EXIT();
}

#define USE_EOF

/** @internal
 * @brief Process a payload transfer    //处理一个有效负载传输
 * 
 * Processes stream, places frames into buffer, signals listeners
 * (such as user callback thread and any polling thread) on new frame
 处理流，将帧转换为缓冲区，信号侦听器
 （如用户回调线程和任何轮询线程）在新的框架上
 *
 * @param payload Contents of the payload transfer, either a packet (isochronous) or a full
 * transfer (bulk mode)
 有效载荷的传递载荷的内容，任何一个数据包（同步）或全转移（散装模式）
 * @param payload_len Length of the payload transfer
 有效载荷的传递payload_len长度
 */
static void _uvc_process_payload(uvc_stream_handle_t *strmh, const uint8_t *payload, size_t const payload_len) {
	size_t header_len;
	uint8_t header_info;
	size_t data_len;
	struct libusb_iso_packet_descriptor *pkt;
	uvc_vc_error_code_control_t vc_error_code;
	uvc_vs_error_code_control_t vs_error_code;

	// magic numbers for identifying header packets from some iSight cameras    //从一些iSight相机识别头包的 幻数(usb数据包的特点格式数值)
	static const uint8_t isight_tag[] = {
		0x11, 0x22, 0x33, 0x44,
		0xde, 0xad, 0xbe, 0xef, 0xde, 0xad, 0xfa, 0xce
	};

	// ignore empty payload transfers   //忽略空的有效负载传输
	if UNLIKELY(!payload || !payload_len || !strmh->outbuf)
		return;

	/* Certain iSight cameras have strange behavior: They send header
	 * information in a packet with no image data, and then the following
  	 * packets have only image data, with no more headers until the next frame.

  	 iSight相机一定有奇怪的行为：他们把标题
     在一个没有图像数据的数据包中的信息，然后
     数据包只有图像数据，没有更多的标题，直到下一帧。
	 *
	 * The iSight header: len(1), flags(1 or 2), 0x11223344(4),
	 * 0xdeadbeefdeadface(8), ??(16)
	*/

	if UNLIKELY(strmh->devh->is_isight &&
		((payload_len < 14) || memcmp(isight_tag, payload + 2, sizeof(isight_tag)) ) &&
		((payload_len < 15) || memcmp(isight_tag, payload + 3, sizeof(isight_tag)) ) ) {
		// The payload transfer doesn't have any iSight magic, so it's all image data
		//有效载荷的传递没有任何的iSight的 幻数(usb数据包的特点格式数值)，所以它的所有图像数据
		header_len = 0;
		data_len = payload_len;
	} else {
		header_len = payload[0];

		if UNLIKELY(header_len > payload_len) {
			strmh->bfh_err |= UVC_STREAM_ERR;
			UVC_DEBUG("bogus packet: actual_len=%zd, header_len=%zd\n", payload_len, header_len);
			return;
		}

		if UNLIKELY(strmh->devh->is_isight)
			data_len = 0;
		else
			data_len = payload_len - header_len;
	}

	if UNLIKELY(header_len < 2) {
		header_info = 0;
	} else {
		//  @todo we should be checking the end-of-header bit
		//我们应该检查头位的结束
		size_t variable_offset = 2;

		header_info = payload[1];

		if UNLIKELY(header_info & UVC_STREAM_ERR) {
//			strmh->bfh_err |= UVC_STREAM_ERR;
			UVC_DEBUG("bad packet: error bit set");     //坏数据包：设置错误位
			libusb_clear_halt(strmh->devh->usb_devh, strmh->stream_if->bEndpointAddress);
//			uvc_vc_get_error_code(strmh->devh, &vc_error_code, UVC_GET_CUR);
			uvc_vs_get_error_code(strmh->devh, &vs_error_code, UVC_GET_CUR);
//			return;
		}

		if ((strmh->fid != (header_info & UVC_STREAM_FID)) && strmh->got_bytes) {
			/* The frame ID bit was flipped, but we have image data sitting
				around from prior transfers. This means the camera didn't send
				an EOF for the last transfer of the previous frame.
				帧标识位被翻转了，但我们有图像数据
                从以前的转让。这意味着相机没有发送
                对前一帧的最后转移EOF。
				*/
			_uvc_swap_buffers(strmh);       //互换holdbuf和outbuf缓冲的视频帧数据 ;
		}

		strmh->fid = header_info & UVC_STREAM_FID;

		if (header_info & UVC_STREAM_PTS) {
			// XXX Mr_Wen some camera may send broken packet or failed to receive all data
			//XXX崎一些相机可以把破包或无法接收所有的数据
			if (LIKELY(variable_offset + 4 <= header_len)) {
				strmh->pts = DW_TO_INT(payload + variable_offset);
				variable_offset += 4;
			} else {
				MARK("bogus packet: header info has UVC_STREAM_PTS, but no data");      //假包：头信息有uvc_stream_scr，但没有数据
				strmh->pts = 0;
			}
		}

		if (header_info & UVC_STREAM_SCR) {
			// @todo read the SOF token counter
			//读SOF令牌计数器
			// XXX Mr_Wen some camera may send broken packet or failed to receive all data
			//XXX崎一些相机可以把破包或无法接收所有的数据
			if (LIKELY(variable_offset + 4 <= header_len)) {
				strmh->last_scr = DW_TO_INT(payload + variable_offset);
				variable_offset += 4;
			} else {
				MARK("bogus packet: header info has UVC_STREAM_SCR, but no data");      //假包：头信息有uvc_stream_scr，但没有数据
				strmh->last_scr = 0;
			}
		}
	}

	if LIKELY(data_len > 0) {
		if LIKELY(strmh->got_bytes + data_len < strmh->size_buf) {
			//memcpy(dest,src,size) 函数用于 把资源内存（src所指向的内存区域） 拷贝到目标内存（dest所指向的内存区域）
			memcpy(strmh->outbuf + strmh->got_bytes, payload + header_len, data_len);
			strmh->got_bytes += data_len;
		} else {
			strmh->bfh_err |= UVC_STREAM_ERR;
		}

		if (header_info & UVC_STREAM_EOF/*(1 << 1)*/) {
			// The EOF bit is set, so publish the complete frame    //第一位被设置，那么出版完整的框架
			_uvc_swap_buffers(strmh);       //互换holdbuf和outbuf缓冲的视频帧数据 ;
		}
	}
}

#if 0
//static inline void _uvc_process_payload_iso(uvc_stream_handle_t *strmh, struct libusb_transfer *transfer) {     //宏定义 #if 0 : 以下代码不编译 ;
//	/* This is an isochronous mode transfer, so each packet has a payload transfer */
//	int packet_id;
//	for (packet_id = 0; packet_id < transfer->num_iso_packets; packet_id++) {
//		struct libusb_iso_packet_descriptor *pkt = transfer->iso_packet_desc + packet_id;
//
//		if UNLIKELY(pkt->status) {
////			UVC_DEBUG("bad packet:status=%d,actual_length=%d", pkt->status, pkt->actual_length);
//			MARK("bad packet:status=%d,actual_length=%d", pkt->status, pkt->actual_length);
//			continue;
//		}
//		if UNLIKELY(!pkt->actual_length) {
//			MARK("zero packet (transfer):");
//			continue;
//		}
//		// libusb_get_iso_packet_buffer_simple will return NULL
//		// libusb_get_iso_packet_buffer_simple 会返回 NULL
//		uint8_t *pktbuf = libusb_get_iso_packet_buffer_simple(transfer, packet_id);
//		_uvc_process_payload(strmh, pktbuf, pkt->actual_length);        //宏定义 #if 0 : 以下代码不编译 ;
//	}
//}
#else
static inline void _uvc_process_payload_iso(uvc_stream_handle_t *strmh, struct libusb_transfer *transfer) {
	/* per packet 每包 */
	uint8_t *pktbuf;
	uint8_t check_header;
	size_t header_len;
	uint8_t header_info;
	struct libusb_iso_packet_descriptor *pkt;

	/* magic numbers for identifying header packets from some iSight cameras
	从一些iSight相机识别头包的 幻数(usb数据包的特点格式数值)*/
	static const uint8_t isight_tag[] = {
		0x11, 0x22, 0x33, 0x44, 0xde, 0xad,
		0xbe, 0xef, 0xde, 0xad, 0xfa, 0xce };
	int packet_id;
	uvc_vc_error_code_control_t vc_error_code;
	uvc_vs_error_code_control_t vs_error_code;

	for (packet_id = 0; packet_id < transfer->num_iso_packets; ++packet_id) {
		check_header = 1;

		pkt = transfer->iso_packet_desc + packet_id;

		if (UNLIKELY(pkt->status != 0)) {
			MARK("bad packet:status=%d,actual_length=%d", pkt->status, pkt->actual_length);
			strmh->bfh_err |= UVC_STREAM_ERR;
			libusb_clear_halt(strmh->devh->usb_devh, strmh->stream_if->bEndpointAddress);
//			uvc_vc_get_error_code(strmh->devh, &vc_error_code, UVC_GET_CUR);
//			uvc_vs_get_error_code(strmh->devh, &vs_error_code, UVC_GET_CUR);
			continue;
		}

		if (UNLIKELY(!pkt->actual_length)) {	// why transfered byte is zero...   //为什么将字节为零…
//			MARK("zero packet (transfer):");
//			strmh->bfh_err |= UVC_STREAM_ERR;	// don't set this flag here     //不要在这里设置这个标志
			continue;
		}
		//定位同步包的位置在一个等时传输缓冲区的便利功能。
		/*
		libusb_get_iso_packet_buffer_simple() :
        transfer	传输流
        packet	返回地址的数据包
        返回 :
        传输缓冲区内的分组缓冲区的基本地址，或者如果数据包不存在的话。
		*/
		// XXX accessing to pktbuf could lead to crash on the original implementation
		// because the substances of pktbuf will be deleted in uvc_stream_stop.
		pktbuf = libusb_get_iso_packet_buffer_simple(transfer, packet_id);
		if (LIKELY(pktbuf)) {	// XXX add null check because libusb_get_iso_packet_buffer_simple could return null
//			assert(pktbuf < transfer->buffer + transfer->length - 1);	// XXX
#ifdef __ANDROID__
			// XXX optimaization because this flag never become true on Android devices
			//XXX优化因为这个标志位永远不会成为真,在Android设备上
			if (UNLIKELY(strmh->devh->is_isight))
#else
			if (strmh->devh->is_isight)
#endif
			{
				if (pkt->actual_length < 30
					|| (memcmp(isight_tag, pktbuf + 2, sizeof(isight_tag))
						&& memcmp(isight_tag, pktbuf + 3, sizeof(isight_tag)))) {
					check_header = 0;
					header_len = 0;
				} else {
					header_len = pktbuf[0];
				}
			} else {
				header_len = pktbuf[0];	// Header length field of Stream Header     //流头的头长度字段
			}

			if (LIKELY(check_header)) {
				header_info = pktbuf[1];
				if (UNLIKELY(header_info & UVC_STREAM_ERR)) {
//					strmh->bfh_err |= UVC_STREAM_ERR;
					MARK("bad packet:status=0x%2x", header_info);
					libusb_clear_halt(strmh->devh->usb_devh, strmh->stream_if->bEndpointAddress);
//					uvc_vc_get_error_code(strmh->devh, &vc_error_code, UVC_GET_CUR);
					uvc_vs_get_error_code(strmh->devh, &vs_error_code, UVC_GET_CUR);
					continue;
				}
#ifdef USE_EOF
				if ((strmh->fid != (header_info & UVC_STREAM_FID)) && strmh->got_bytes) {	// got_bytesを取ると殆ど画面更新されない
				/* The frame ID bit was flipped, but we have image data sitting
	             around from prior transfers. This means the camera didn't send
    		     an EOF for the last transfer of the previous frame or some frames losted.
    		     帧标识位被翻转了，但我们有图像数据
                 从以前的转让。这意味着相机没有发送
                 对前一帧或帧的最后转移EOF丢失。
    		     */
					_uvc_swap_buffers(strmh);       //互换holdbuf和outbuf缓冲的视频帧数据 ;
				}
				strmh->fid = header_info & UVC_STREAM_FID;
#else
				if (strmh->fid != (header_info & UVC_STREAM_FID)) {	// when FID is toggled  //当FID拨动
					_uvc_swap_buffers(strmh);       //互换holdbuf和outbuf缓冲的视频帧数据 ;
					strmh->fid = header_info & UVC_STREAM_FID;
				}
#endif
				if (header_info & UVC_STREAM_PTS) {
					// XXX Mr_Wen some camera may send broken packet or failed to receive all data
					//XXX崎一些相机可以把破包或无法接收所有的数据
					if (LIKELY(header_len >= 6)) {
						strmh->pts = DW_TO_INT(pktbuf + 2);
					} else {
						MARK("bogus packet: header info has UVC_STREAM_PTS, but no data");
						strmh->pts = 0;
					}
				}

				if (header_info & UVC_STREAM_SCR) {
					// XXX Mr_Wen some camera may send broken packet or failed to receive all data
					if (LIKELY(header_len >= 10)) {
						strmh->last_scr = DW_TO_INT(pktbuf + 6);
					} else {
						MARK("bogus packet: header info has UVC_STREAM_SCR, but no data");  //假包：头信息有uvc_stream_scr，但没有数据
						strmh->last_scr = 0;
					}
				}

#ifdef __ANDROID__	// XXX optimaization because this flag never become true on Android devices
				if (UNLIKELY(strmh->devh->is_isight))
					continue; // don't look for data after an iSight header     //iSight头后数据不看
#else
				if (strmh->devh->is_isight) {
					MARK("is_isight");
					continue; // don't look for data after an iSight header
				}
#endif
			} // if LIKELY(check_header)

			if (UNLIKELY(pkt->actual_length < header_len)) {
				/* Bogus packet received
				伪造的数据包的接收*/
				strmh->bfh_err |= UVC_STREAM_ERR;
				MARK("bogus packet: actual_len=%d, header_len=%zd", pkt->actual_length, header_len);
				continue;
			}

			// XXX original implementation could lead to trouble because unsigned values
			// and there calculated value never become minus.
			// therefor changed to "if (pkt->actual_length > header_len)"
			// from "if (pkt->actual_length - header_len > 0)"
			/*
			原始的实现可能会导致麻烦，因为无符号值
            并且计算值不会变为负数。
            因此，改为“如果（PKT -> actual_length > header_len）”
            “如果（PKT -> actual_length - header_len > 0）”
			*/
			if (LIKELY(pkt->actual_length > header_len)) {
				const size_t odd_bytes = pkt->actual_length - header_len;
				//assert()宏的作用是如果它的条件返回错误，则终止程序执行
				assert(strmh->got_bytes + odd_bytes < strmh->size_buf);
				assert(strmh->outbuf);
				assert(pktbuf);
				//memcpy(dest,src,size) 函数用于 把资源内存（src所指向的内存区域） 拷贝到目标内存（dest所指向的内存区域）
				memcpy(strmh->outbuf + strmh->got_bytes, pktbuf + header_len, odd_bytes);
				strmh->got_bytes += odd_bytes;
			}
#ifdef USE_EOF
			if ((pktbuf[1] & UVC_STREAM_EOF) && strmh->got_bytes != 0) {
				/* The EOF bit is set, so publish the complete frame
				第一位被设置，那么出版完整的框架*/
				_uvc_swap_buffers(strmh);       //互换holdbuf和outbuf缓冲的视频帧数据 ;
			}
#endif
		} else {	// if (LIKELY(pktbuf))
			strmh->bfh_err |= UVC_STREAM_ERR;
			MARK("libusb_get_iso_packet_buffer_simple returned null");
			continue;
		}
	}	// for
}
#endif

/** @internal
 * @brief Isochronous transfer callback
 * 
 * Processes stream, places frames into buffer, signals listeners
 * (such as user callback thread and any polling thread) on new frame

 处理流，将帧转换为缓冲区，信号侦听器
 （如用户回调线程和任何轮询线程）在新的框架上
 *
 * @param transfer Active transfer  //主动转移
 */
static void _uvc_stream_callback(struct libusb_transfer *transfer) {    //_uvc_stream_callback相机数据流回调 :
	if UNLIKELY(!transfer) return;

	uvc_stream_handle_t *strmh = transfer->user_data;       //强转为uvc_stream_handle_t
	if UNLIKELY(!strmh) return;

	int resubmit = 1;

#ifndef NDEBUG
	static int cnt = 0;
	if UNLIKELY((++cnt % 1000) == 0)
		MARK("cnt=%d", cnt);
#endif
	switch (transfer->status) {     //根据数据流的transfer->status状态位判断进行相应的操作 ;
	case LIBUSB_TRANSFER_COMPLETED:     //libusb转移完成 :
		if (!transfer->num_iso_packets) {
			/* This is a bulk mode transfer, so it just has one payload transfer
			这是一个批量模式传输，所以它只是有一个有效载荷传输*/
			_uvc_process_payload(strmh, transfer->buffer, transfer->actual_length);
		} else {
			/* This is an isochronous mode transfer, so each packet has a payload transfer
			这是一个同步传输模式，所以每个数据包有效载荷传递*/
			_uvc_process_payload_iso(strmh, transfer);
		}
	    break;
	case LIBUSB_TRANSFER_NO_DEVICE:     //libusb的传输没有设备 :
		strmh->running = 0;	// this needs for unexpected disconnect of cable otherwise hangup   //这需要对意外断开电缆否则挂断
		// pass through to following lines      //通过以下线路
	case LIBUSB_TRANSFER_CANCELLED:     //libusb传输取消
	case LIBUSB_TRANSFER_ERROR:     //libusb的传输错误
		UVC_DEBUG("not retrying transfer, status = %d", transfer->status);
//		MARK("not retrying transfer, status = %d", transfer->status);
//		_uvc_delete_transfer(transfer);
		resubmit = 0;
		break;
	case LIBUSB_TRANSFER_TIMED_OUT:     //libusb传输超时
	case LIBUSB_TRANSFER_STALL:     //libusb转让摊位
	case LIBUSB_TRANSFER_OVERFLOW:      //libusb传输溢出
		UVC_DEBUG("retrying transfer, status = %d", transfer->status);
//		MARK("retrying transfer, status = %d", transfer->status);
		break;
	}

	if (LIKELY(strmh->running && resubmit)) {
	//提交transfer中的数据到usb中 :
		libusb_submit_transfer(transfer);
	} else {
		// XXX delete non-reusing transfer
		// real implementation of deleting transfer moves to _uvc_delete_transfer
		_uvc_delete_transfer(transfer);
	}
}

#if 0       //这个宏定义内的 _uvc_iso_callback()不会被编译 :
/** @internal
 * @brief Isochronous transfer callback
 * 
 * Processes stream, places frames into buffer, signals listeners
 * (such as user callback thread and any polling thread) on new frame
 处理流，将帧转换为缓冲区，信号侦听器
 （如用户回调线程和任何轮询线程）在新的框架上
 *
 * @param transfer Active transfer
 */
//static void _uvc_iso_callback(struct libusb_transfer *transfer) {    //因为 #if 0  : 这个宏定义内的 _uvc_iso_callback()不会被编译 :
//	uvc_stream_handle_t *strmh;
//	int packet_id;
//
//	/* per packet */
//	uint8_t *pktbuf;
//	uint8_t check_header;
//	size_t header_len;	// XXX unsigned int header_len
//	uint8_t header_info;
//	struct libusb_iso_packet_descriptor *pkt;
//
//	/* magic numbers for identifying header packets from some iSight cameras */ //幻数(usb数据包的特点格式数值)
//	static const uint8_t isight_tag[] = {
//		0x11, 0x22, 0x33, 0x44, 0xde, 0xad,
//		0xbe, 0xef, 0xde, 0xad, 0xfa, 0xce };
//
//	strmh = transfer->user_data;
//#ifndef NDEBUG
//	static int cnt = 0;
//	if ((++cnt % 1000) == 0)
//		MARK("cnt=%d", cnt);
//#endif
//	switch (transfer->status) {
//	case LIBUSB_TRANSFER_COMPLETED:
//		if (UNLIKELY(!transfer->num_iso_packets))
//			MARK("num_iso_packets is zero");
//		for (packet_id = 0; packet_id < transfer->num_iso_packets; ++packet_id) {
//			check_header = 1;
//
//			pkt = transfer->iso_packet_desc + packet_id;
//
//			if (UNLIKELY(pkt->status != 0)) {
//				MARK("bad packet:status=%d,actual_length=%d", pkt->status, pkt->actual_length);
//				strmh->bfh_err |= UVC_STREAM_ERR;
//				continue;
//			}
//
//			if (UNLIKELY(!pkt->actual_length)) {	// why transfered byte is zero...
////				MARK("zero packet (transfer):");
////				strmh->bfh_err |= UVC_STREAM_ERR;	// don't set this flag here
//				continue;
//			}
//			// XXX accessing to pktbuf could lead to crash on the original implementation
//			// because the substances of pktbuf will be deleted in uvc_stream_stop.
//			pktbuf = libusb_get_iso_packet_buffer_simple(transfer, packet_id);
//			if (LIKELY(pktbuf)) {	// XXX add null check because libusb_get_iso_packet_buffer_simple could return null
////				assert(pktbuf < transfer->buffer + transfer->length - 1);	// XXX
//#ifdef __ANDROID__
//				// XXX optimaization because this flag never become true on Android devices
//				if (UNLIKELY(strmh->devh->is_isight))
//#else
//				if (strmh->devh->is_isight)
//#endif
//				{
//					if (pkt->actual_length < 30
//						|| (memcmp(isight_tag, pktbuf + 2, sizeof(isight_tag))
//							&& memcmp(isight_tag, pktbuf + 3, sizeof(isight_tag)))) {
//						check_header = 0;
//						header_len = 0;
//					} else {
//						header_len = pktbuf[0];
//					}
//				} else {
//					header_len = pktbuf[0];	// Header length field of Stream Header
//				}
//
//				if (LIKELY(check_header)) {
//					header_info = pktbuf[1];
//					if (UNLIKELY(header_info & UVC_STREAM_ERR)) {
//						strmh->bfh_err |= UVC_STREAM_ERR;
//						MARK("bad packet");
////						libusb_clear_halt(strmh->devh->usb_devh, strmh->stream_if->bEndpointAddress);
//						uvc_vc_get_error_code(strmh->devh, &vc_error_code, UVC_GET_CUR);
//						uvc_vs_get_error_code(strmh->devh, &vs_error_code, UVC_GET_CUR);
//						continue;
//					}
//#ifdef USE_EOF
//					if ((strmh->fid != (header_info & UVC_STREAM_FID)) && strmh->got_bytes) {	// got_bytesを取ると殆ど画面更新されない
//					/* The frame ID bit was flipped, but we have image data sitting
//		             around from prior transfers. This means the camera didn't send
//        		     an EOF for the last transfer of the previous frame or some frames losted. */
//						_uvc_swap_buffers(strmh);
//					}
//					strmh->fid = header_info & UVC_STREAM_FID;
//#else
//					if (strmh->fid != (header_info & UVC_STREAM_FID)) {	// when FID is toggled
//						_uvc_swap_buffers(strmh);
//						strmh->fid = header_info & UVC_STREAM_FID;
//					}
//#endif
//					if (header_info & UVC_STREAM_PTS) {
//						// XXX Mr_Wen some camera may send broken packet or failed to receive all data
//						if (LIKELY(header_len >= 6)) {
//							strmh->pts = DW_TO_INT(pktbuf + 2);
//						} else {
//							MARK("bogus packet: header info has UVC_STREAM_PTS, but no data");
//							strmh->pts = 0;
//						}
//					}
//
//					if (header_info & UVC_STREAM_SCR) {
//						// XXX Mr_Wen some camera may send broken packet or failed to receive all data
//						if (LIKELY(header_len >= 10)) {
//							strmh->last_scr = DW_TO_INT(pktbuf + 6);
//						} else {
//							MARK("bogus packet: header info has UVC_STREAM_SCR, but no data");
//							strmh->last_scr = 0;
//						}
//					}
//
//#ifdef __ANDROID__	// XXX optimaization because this flag never become true on Android devices
//					if (UNLIKELY(strmh->devh->is_isight))
//						continue; // don't look for data after an iSight header
//#else
//					if (strmh->devh->is_isight) {
//						MARK("is_isight");
//						continue; // don't look for data after an iSight header
//					}
//#endif
//				} // if LIKELY(check_header)
//
//				if (UNLIKELY(pkt->actual_length < header_len)) {
//					/* Bogus packet received */
//					strmh->bfh_err |= UVC_STREAM_ERR;
//					MARK("bogus packet: actual_len=%d, header_len=%zd", pkt->actual_length, header_len);
//					continue;
//				}
//
//				// XXX original implementation could lead to trouble because unsigned values
//				// and there calculated value never become minus.
//				// therefor changed to "if (pkt->actual_length > header_len)"
//				// from "if (pkt->actual_length - header_len > 0)"
//				if (LIKELY(pkt->actual_length > header_len)) {
//					const size_t odd_bytes = pkt->actual_length - header_len;
//					assert(strmh->got_bytes + odd_bytes < strmh->size_buf);
//					assert(strmh->outbuf);
//					assert(pktbuf);
//					// ??
//					memcpy(strmh->outbuf + strmh->got_bytes, pktbuf + header_len, odd_bytes);   //因为 #if 0  : 这个宏定义内的 _uvc_iso_callback()不会被编译 :
//					strmh->got_bytes += odd_bytes;
//				}
//#ifdef USE_EOF
//				if ((pktbuf[1] & STREAM_HEADER_BFH_EOF) && strmh->got_bytes != 0) {
//					/* The EOF bit is set, so publish the complete frame */
//					_uvc_swap_buffers(strmh);
//				}
//#endif
//			} else {	// if (LIKELY(pktbuf))
//				strmh->bfh_err |= UVC_STREAM_ERR;
//				MARK("libusb_get_iso_packet_buffer_simple returned null");
//				continue;
//			}
//		}	// for
//		break;
//	case LIBUSB_TRANSFER_NO_DEVICE:
//		strmh->running = 0;	// this needs for unexpected disconnect of cable otherwise hangup
//	case LIBUSB_TRANSFER_CANCELLED:
//	case LIBUSB_TRANSFER_ERROR:
//		UVC_DEBUG("not retrying transfer, status = %d", transfer->status);
////		MARK("not retrying transfer, status = %d", transfer->status);
//		_uvc_delete_transfer(transfer);
//		break;
//	case LIBUSB_TRANSFER_TIMED_OUT:
//	case LIBUSB_TRANSFER_STALL:
//	case LIBUSB_TRANSFER_OVERFLOW:
//		UVC_DEBUG("retrying transfer, status = %d", transfer->status);
////		MARK("retrying transfer, status = %d", transfer->status);
//		break;
//	}
//
//	if (LIKELY(strmh->running)) {
//		libusb_submit_transfer(transfer);
//	} else {
//		// XXX delete non-reusing transfer
//		// real implementation of deleting transfer moves to _uvc_delete_transfer
//		_uvc_delete_transfer(transfer);
//	}
//}
#endif

/** Begin streaming video from the camera into the callback function.
 * @ingroup streaming
 *
 * @param devh UVC device
 * @param ctrl Control block, processed using {uvc_probe_stream_ctrl} or
 *             {uvc_get_stream_ctrl_format_size}
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 * @param flags Stream setup flags, currently undefined. Set this to zero. The lower bit
 * is reserved for backward compatibility.
 */
uvc_error_t uvc_start_streaming(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, uvc_frame_callback_t *cb, void *user_ptr,
		uint8_t flags) {
	return uvc_start_streaming_bandwidth(devh, ctrl, cb, user_ptr, 0, flags);
}

/** Begin streaming video from the camera into the callback function.
 * @ingroup streaming
 *
 * @param devh UVC device
 * @param ctrl Control block, processed using {uvc_probe_stream_ctrl} or
 *             {uvc_get_stream_ctrl_format_size}
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 * @param bandwidth_factor [0.0f, 1.0f]
 * @param flags Stream setup flags, currently undefined. Set this to zero. The lower bit
 * is reserved for backward compatibility.
 */
 //UVC开始流带宽    在UVCPreview的do_preview()中被调用 :
uvc_error_t uvc_start_streaming_bandwidth(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, uvc_frame_callback_t *cb, void *user_ptr,
		float bandwidth_factor,
		uint8_t flags) {
	uvc_error_t ret;
	uvc_stream_handle_t *strmh;     //2016.08.30 : uvc_stream_handle_t数据结构第一次被创建并传递 , 源数据对象 ;

	ret = uvc_stream_open_ctrl(devh, &strmh, ctrl);
	if (UNLIKELY(ret != UVC_SUCCESS))
		return ret;

	ret = uvc_stream_start_bandwidth(strmh, cb, user_ptr, bandwidth_factor, flags);
	if (UNLIKELY(ret != UVC_SUCCESS)) {
		uvc_stream_close(strmh);
		return ret;
	}

	return UVC_SUCCESS;
}

/** Begin streaming video from the camera into the callback function.
 * @ingroup streaming
 *
 * @deprecated The stream type (bulk vs. isochronous) will be determined by the
 * type of interface associated with the uvc_stream_ctrl_t parameter, regardless
 * of whether the caller requests isochronous streaming. Please switch to
 * uvc_start_streaming().
 *
 * @param devh UVC device
 * @param ctrl Control block, processed using {uvc_probe_stream_ctrl} or
 *             {uvc_get_stream_ctrl_format_size}
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 */
uvc_error_t uvc_start_iso_streaming(uvc_device_handle_t *devh,
		uvc_stream_ctrl_t *ctrl, uvc_frame_callback_t *cb, void *user_ptr) {
	return uvc_start_streaming_bandwidth(devh, ctrl, cb, user_ptr, 0.0f, 0);
}

static uvc_stream_handle_t *_uvc_get_stream_by_interface(
		uvc_device_handle_t *devh, int interface_idx) {
	uvc_stream_handle_t *strmh;

	DL_FOREACH(devh->streams, strmh)
	{
		if (strmh->stream_if->bInterfaceNumber == interface_idx)
			return strmh;
	}

	return NULL;
}

static uvc_streaming_interface_t *_uvc_get_stream_if(uvc_device_handle_t *devh,
		int interface_idx) {
	uvc_streaming_interface_t *stream_if;

	DL_FOREACH(devh->info->stream_ifs, stream_if)
	{
		if (stream_if->bInterfaceNumber == interface_idx)
			return stream_if;
	}

	return NULL;
}

/** Open a new video stream.    //打开一个新的视频流。
 * @ingroup streaming
 *
 * @param devh UVC device
 * @param ctrl Control block, processed using {uvc_probe_stream_ctrl} or
 *             {uvc_get_stream_ctrl_format_size}
 */
uvc_error_t uvc_stream_open_ctrl(uvc_device_handle_t *devh,
		uvc_stream_handle_t **strmhp, uvc_stream_ctrl_t *ctrl) {
	/* Chosen frame and format descriptors //选择的帧和格式描述符   */
	uvc_stream_handle_t *strmh = NULL;
	uvc_streaming_interface_t *stream_if;
	uvc_error_t ret;

	UVC_ENTER();

	if (UNLIKELY(_uvc_get_stream_by_interface(devh, ctrl->bInterfaceNumber) != NULL)) {
		ret = UVC_ERROR_BUSY; /* Stream is already opened  //流已打开 */
		goto fail;
	}

	stream_if = _uvc_get_stream_if(devh, ctrl->bInterfaceNumber);
	if (UNLIKELY(!stream_if)) {
		ret = UVC_ERROR_INVALID_PARAM;
		goto fail;
	}

	strmh = calloc(1, sizeof(*strmh));      //为strmh申请内存区域,并清零 ;
	if (UNLIKELY(!strmh)) {
		ret = UVC_ERROR_NO_MEM;
		goto fail;
	}
	strmh->devh = devh;
	strmh->stream_if = stream_if;
	strmh->frame.library_owns_data = 1;

	ret = uvc_claim_if(strmh->devh, strmh->stream_if->bInterfaceNumber);
	if (UNLIKELY(ret != UVC_SUCCESS))
		goto fail;

	ret = uvc_stream_ctrl(strmh, ctrl);
	if (UNLIKELY(ret != UVC_SUCCESS))
		goto fail;

	// Set up the streaming status and data space   //建立了流媒体状态和数据空间
	strmh->running = 0;
	/** @todo take only what we need
	//只需要我们所需要的 */
	//outbuf和holdbuf都是图像帧数据缓冲区 :
	strmh->outbuf = malloc(LIBUVC_XFER_BUF_SIZE);   //
	strmh->holdbuf = malloc(LIBUVC_XFER_BUF_SIZE);
	strmh->size_buf = LIBUVC_XFER_BUF_SIZE;	// xxx for boundary check   //用于边界检查

	pthread_mutex_init(&strmh->cb_mutex, NULL);
	pthread_cond_init(&strmh->cb_cond, NULL);

	DL_APPEND(devh->streams, strmh);

	*strmhp = strmh;

	UVC_EXIT(0);
	return UVC_SUCCESS;

fail:
	if (strmh)
		free(strmh);
	UVC_EXIT(ret);
	return ret;
}

/** Begin streaming video from the stream into the callback function.
 * @ingroup streaming
 *
 * @param strmh UVC stream
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 * @param flags Stream setup flags, currently undefined. Set this to zero. The lower bit
 * is reserved for backward compatibility.
 */
 //该函数未被实际调用 :
uvc_error_t uvc_stream_start(uvc_stream_handle_t *strmh,
		uvc_frame_callback_t *cb, void *user_ptr, uint8_t flags) {
	return uvc_stream_start_bandwidth(strmh, cb, user_ptr, 0, flags);
}

/** Begin streaming video from the stream into the callback function.
从流开始流视频到回调函数。
 * @ingroup streaming
 *
 * @param strmh UVC stream  //UVC流
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 * @param bandwidth_factor [0.0f, 1.0f]
 * @param flags Stream setup flags, currently undefined. Set this to zero. The lower bit
 * is reserved for backward compatibility.
 流设置标志，当前未定义。将此设置为零。下点
 是保留向后兼容性的
 */
 //******************************************************************
 //2016.08.24 : 重要的函数 , 预览帧回调方法传递到了这里 :
uvc_error_t uvc_stream_start_bandwidth(uvc_stream_handle_t *strmh,
		uvc_frame_callback_t *cb, void *user_ptr, float bandwidth_factor, uint8_t flags) {
	/* USB interface we'll be using */
	//我们将使用USB接口
	const struct libusb_interface *interface;
	int interface_id;
	char isochronous;
	uvc_frame_desc_t *frame_desc;
	uvc_format_desc_t *format_desc;
	uvc_stream_ctrl_t *ctrl;
	uvc_error_t ret;
	/* Total amount of data per transfer */
	//每次传输的数据总量
	size_t total_transfer_size;
	struct libusb_transfer *transfer;
	int transfer_id;

	ctrl = &strmh->cur_ctrl;

	UVC_ENTER();

	if (UNLIKELY(strmh->running)) {
		UVC_EXIT(UVC_ERROR_BUSY);
		return UVC_ERROR_BUSY;
	}

	strmh->running = 1;
	strmh->seq = 0;
	strmh->fid = 0;
	strmh->pts = 0;
	strmh->last_scr = 0;
	strmh->bfh_err = 0;	// XXX

	frame_desc = uvc_find_frame_desc_stream(strmh, ctrl->bFormatIndex, ctrl->bFrameIndex);
	if (UNLIKELY(!frame_desc)) {
		ret = UVC_ERROR_INVALID_PARAM;
		LOGE("UVC_ERROR_INVALID_PARAM");
		goto fail;
	}
	format_desc = frame_desc->parent;

	strmh->frame_format = uvc_frame_format_for_guid(format_desc->guidFormat);
	if (UNLIKELY(strmh->frame_format == UVC_FRAME_FORMAT_UNKNOWN)) {
		ret = UVC_ERROR_NOT_SUPPORTED;
		LOGE("unlnown frame format");       //未知的帧格式
		goto fail;
	}
	const uint32_t dwMaxVideoFrameSize = ctrl->dwMaxVideoFrameSize <= frame_desc->dwMaxVideoFrameBufferSize
		? ctrl->dwMaxVideoFrameSize : frame_desc->dwMaxVideoFrameBufferSize;

	// Get the interface that provides the chosen format and frame configuration
	//获取提供所选择的格式和帧配置的接口
	interface_id = strmh->stream_if->bInterfaceNumber;
	interface = &strmh->devh->info->config->interface[interface_id];

	/* A VS interface uses isochronous transfers if it has multiple altsettings.
	他用等时界面与转移如果它有多altsettings
	 * (UVC 1.5: 2.4.3. VideoStreaming Interface, on page 19) //视频接口，19页 */
	isochronous = interface->num_altsetting > 1;

	if (isochronous) {      //等时传输模式
		MARK("isochronous transfer mode");      //等时传输模式
		/* For isochronous streaming, we choose an appropriate altsetting for the endpoint
		 * and set up several transfers */
		 //同步流，我们选择一个合适的altsetting端点建立几个转移
		const struct libusb_interface_descriptor *altsetting;
		const struct libusb_endpoint_descriptor *endpoint;
		/* The greatest number of bytes that the device might provide, per packet, in this
		 * configuration */
		 //该设备可以提供的最大字节数，每包，在这个配置
		size_t config_bytes_per_packet;
		/* Number of packets per transfer */
		//每传输的数据包数
		size_t packets_per_transfer;
		/* Total amount of data per transfer */
		//每次传输的数据总量
		size_t total_transfer_size;
		/* Size of packet transferable from the chosen endpoint */
		//从所选择的端点转移的数据包的大小
		size_t endpoint_bytes_per_packet;
		/* Index of the altsetting */
		//该altsetting指数
		int alt_idx, ep_idx;

		struct libusb_transfer *transfer;
		int transfer_id;
		
		if ((bandwidth_factor > 0) && (bandwidth_factor < 1.0f)) {
			config_bytes_per_packet = (size_t)(strmh->cur_ctrl.dwMaxPayloadTransferSize * bandwidth_factor);
			if (!config_bytes_per_packet) {
				config_bytes_per_packet = strmh->cur_ctrl.dwMaxPayloadTransferSize;
			}
		} else {
			config_bytes_per_packet = strmh->cur_ctrl.dwMaxPayloadTransferSize;
		}
//#if !defined(__LP64__)
//		LOGI("config_bytes_per_packet=%d", config_bytes_per_packet);
//#else
//		LOGI("config_bytes_per_packet=%ld", config_bytes_per_packet);
//#endif

		if (UNLIKELY(!config_bytes_per_packet)) {	// XXX added to privent zero divided exception at the following code   // XXX加入私人零分例外，在下面的代码
			ret = UVC_ERROR_IO;
			LOGE("config_bytes_per_packet is zero");    //每个包的配置字节为零
			goto fail;
		}

		/* Go through the altsettings and find one whose packets are at least
		 * as big as our format's maximum per-packet usage. Assume that the
		 * packet sizes are increasing.

		 经过altsettings找到一个包至少
         和我们的格式的最大数据包的使用一样大。假设
         数据包大小正在增加。
		 */
		const int num_alt = interface->num_altsetting - 1;
		for (alt_idx = 0; alt_idx <= num_alt ; alt_idx++) {
			altsetting = interface->altsetting + alt_idx;
			endpoint_bytes_per_packet = 0;

			/* Find the endpoint with the number specified in the VS header */
			//查找与在“与”头中指定的数字的端点
			for (ep_idx = 0; ep_idx < altsetting->bNumEndpoints; ep_idx++) {
				endpoint = altsetting->endpoint + ep_idx;
				if (endpoint->bEndpointAddress == format_desc->parent->bEndpointAddress) {
					endpoint_bytes_per_packet = endpoint->wMaxPacketSize;
					// wMaxPacketSize: [unused:2 (multiplier-1):3 size:11]
					// bit10…0:		maximum packet size 最大数据包大小
					// bit12…11:	the number of additional transaction opportunities per microframe for high-speed    //号额外的交易机会，为每microframe高速
					//				00 = None (1 transaction per microframe)    //没有（每微帧1交易）
					//				01 = 1 additional (2 per microframe)        //1（2个额外的microframe）
					//				10 = 2 additional (3 per microframe)        //额外的2（3元microframe）
					//				11 = Reserved       //保留
					endpoint_bytes_per_packet
						= (endpoint_bytes_per_packet & 0x07ff)
							* (((endpoint_bytes_per_packet >> 11) & 3) + 1);
					break;
				}
			}
			// XXX config_bytes_per_packet should not be zero otherwise zero divided exception occur    //XXX config_bytes_per_packet不宜零否则零分例外发生
			if (LIKELY(endpoint_bytes_per_packet)) {
				if ( (endpoint_bytes_per_packet >= config_bytes_per_packet)
					|| (alt_idx == num_alt) ) {	        // XXX always match to last altsetting for buggy device //XXX总是匹配的马车上altsetting装置
					/* Transfers will be at most one frame long: Divide the maximum frame size
					 * by the size of the endpoint and round up
					 传输将在最长的一个框架：划分最大帧大小
                     由端点和圆的大小
					 */
					packets_per_transfer = (dwMaxVideoFrameSize
							+ endpoint_bytes_per_packet - 1)
							/ endpoint_bytes_per_packet;		// XXX cashed by zero divided exception occured     //XXX兑现零分发生异常

					/* But keep a reasonable limit: Otherwise we start dropping data
					但保持一个合理的限制：否则，我们开始下降数据
					*/
					if (packets_per_transfer > 32)
						packets_per_transfer = 32;

					total_transfer_size = packets_per_transfer * endpoint_bytes_per_packet;
					break;
				}
			}
		}
		if (UNLIKELY(!endpoint_bytes_per_packet)) {
			LOGE("endpoint_bytes_per_packet is zero");      //每个包的端点字节数为零
			ret = UVC_ERROR_INVALID_MODE;
			goto fail;
		}
		if (UNLIKELY(!total_transfer_size)) {
			LOGE("total_transfer_size is zero");        //总的大小是零转移
			ret = UVC_ERROR_INVALID_MODE;
			goto fail;
		}

		/* If we searched through all the altsettings and found nothing usable  //如果我们找遍了所有的设置，并没有发现任何可用的ALT */
/*		if (UNLIKELY(alt_idx == interface->num_altsetting)) {	// XXX never hit this condition     //XXX不命中本状态
			UVC_DEBUG("libusb_set_interface_alt_setting failed");   //libusb设置界面ALT设置失败
			ret = UVC_ERROR_INVALID_MODE;
			goto fail;
		} */

		/* Select the altsetting
		选择ALT设置
		*/
		MARK("Select the altsetting");      //选择ALT设置
		ret = libusb_set_interface_alt_setting(strmh->devh->usb_devh,
				altsetting->bInterfaceNumber, altsetting->bAlternateSetting);
		if (UNLIKELY(ret != UVC_SUCCESS)) {
			UVC_DEBUG("libusb_set_interface_alt_setting failed");       //libusb设置界面ALT设置失败
			goto fail;
		}

		/* Set up the transfers     //建立传输 */
		MARK("Set up the transfers");       //建立传输
		for (transfer_id = 0; transfer_id < LIBUVC_NUM_TRANSFER_BUFS; ++transfer_id) {
			transfer = libusb_alloc_transfer(packets_per_transfer);
			strmh->transfers[transfer_id] = transfer;
			strmh->transfer_bufs[transfer_id] = malloc(total_transfer_size);

            //2016.08.26 : 在此进行usb数据的通讯传输 :
            //填充所需的libusb_transfer领域一个等时传输的辅助函数。
            /*
            libusb_fill_iso_transfer() :
            transfer	转移到填充
            dev_handle	将处理传输的设备的句柄
            endpoint	此传输将被发送的端点的地址
            buffer	数据缓冲区
            length	数据缓冲区长度
            num_iso_packets	等时数据包数
            callback	在传输完成时调用的回调函数
            user_data	用户数据传递到回调函数
            timeout	以毫秒为单位的传输超时
            */
			libusb_fill_iso_transfer(transfer, strmh->devh->usb_devh,
				format_desc->parent->bEndpointAddress,
				strmh->transfer_bufs[transfer_id], total_transfer_size,
				packets_per_transfer, _uvc_stream_callback,     //_uvc_stream_callback相机数据流回调 :
				(void*) strmh, 5000);

			libusb_set_iso_packet_lengths(transfer, endpoint_bytes_per_packet);
		}
	} else {
		MARK("bulk transfer mode");     //批量传输模式
		/** prepare for bulk transfer   //准备批量传输 */
		for (transfer_id = 0; transfer_id < LIBUVC_NUM_TRANSFER_BUFS; ++transfer_id) {
			transfer = libusb_alloc_transfer(0);
			strmh->transfers[transfer_id] = transfer;
			strmh->transfer_bufs[transfer_id] = malloc(strmh->cur_ctrl.dwMaxPayloadTransferSize);

			//2016.08.26 : 在此进行usb数据的通讯传输 :
			//libusb_fill_bulk_transfer() :填充所需的libusb_transfer领域数据块传输的辅助函数。
			/*
			libusb_fill_bulk_transfer() :
            transfer	转移到填充
            dev_handle	将处理传输的设备的句柄
            endpoint	此传输将被发送的端点的地址
            buffer	数据缓冲区
            length	数据缓冲区长度
            callback	在传输完成时调用的回调函数
            user_data	用户数据传递到回调函数
            timeout	以毫秒为单位的传输超时
			*/
			libusb_fill_bulk_transfer(transfer, strmh->devh->usb_devh,
				format_desc->parent->bEndpointAddress,
				strmh->transfer_bufs[transfer_id],
				strmh->cur_ctrl.dwMaxPayloadTransferSize, _uvc_stream_callback,     //_uvc_stream_callback相机数据流回调 :
				(void *)strmh, 5000);

		}
	}

	strmh->user_cb = cb;    //视频数据帧回调函数赋值 :
	strmh->user_ptr = user_ptr;

	/* If the user wants it, set up a thread that calls the user's function
	 * with the contents of each frame.

	 如果用户想要它，设置一个调用用户函数的线程
     随着每一帧的内容。
	 */
	MARK("create callback thread");     //创建回调线程
	if LIKELY(cb) {
	/*
	pthread_create是类Unix操作系统（Unix、Linux、Mac OS X等）的创建线程的函数
	第一个参数为指向线程标识符的指针。
	第二个参数用来设置线程属性。
	第三个参数是线程运行函数的起始地址。
	最后一个参数是运行函数的参数。
	*/
		pthread_create(&strmh->cb_thread, NULL, _uvc_user_caller, (void*) strmh);   //启动独立线程 :
	}
	MARK("submit transfers");       //提交转让
	for (transfer_id = 0; transfer_id < LIBUVC_NUM_TRANSFER_BUFS; transfer_id++) {
	//将transfers[]数组里的transfers轮询提交发送到usb
		ret = libusb_submit_transfer(strmh->transfers[transfer_id]);

		if (UNLIKELY(ret != UVC_SUCCESS)) {
			UVC_DEBUG("libusb_submit_transfer failed");     //libusb提交转移失败
			break;
		}
	}

	if (UNLIKELY(ret != UVC_SUCCESS)) {
		/** @todo clean up transfers and memory
		清理传输和内存
		*/
		goto fail;
	}

	UVC_EXIT(ret);
	return ret;
fail:
	LOGE("fail");
	strmh->running = 0;
	UVC_EXIT(ret);
	return ret;
}

//******************************************************************

/** Begin streaming video from the stream into the callback function.
从流开始流视频到回调函数。
 * @ingroup streaming
 *
 * @deprecated The stream type (bulk vs. isochronous) will be determined by the
 * type of interface associated with the uvc_stream_ctrl_t parameter, regardless
 * of whether the caller requests isochronous streaming. Please switch to
 * uvc_stream_start().
 *
 * @param strmh UVC stream
 * @param cb   User callback function. See {uvc_frame_callback_t} for restrictions.
 */
uvc_error_t uvc_stream_start_iso(uvc_stream_handle_t *strmh,
		uvc_frame_callback_t *cb, void *user_ptr) {
	return uvc_stream_start(strmh, cb, user_ptr, 0);
}

/** @internal
 * @brief User callback runner thread
 * @note There should be at most one of these per currently streaming device
 * @param arg Device handle
 */
 //2016.08.30 : strmh->user_cb在此回调 :
static void *_uvc_user_caller(void *arg) {
	uvc_stream_handle_t *strmh = (uvc_stream_handle_t *) arg;

	uint32_t last_seq = 0;

	for (; 1 ;) {
		pthread_mutex_lock(&strmh->cb_mutex);
		{


			for (; strmh->running && (last_seq == strmh->hold_seq) ;) {
				pthread_cond_wait(&strmh->cb_cond, &strmh->cb_mutex);
				usleep(1000);//20180418 thread sleep one milliseconds
			}

			if (UNLIKELY(!strmh->running)) {
				pthread_mutex_unlock(&strmh->cb_mutex);
				break;
			}

			last_seq = strmh->hold_seq;
			if (LIKELY(!strmh->hold_bfh_err))	// XXX
				_uvc_populate_frame(strmh);     //UVC填充数据帧frame 重要 ;
		}
		pthread_mutex_unlock(&strmh->cb_mutex);

		if (LIKELY(!strmh->hold_bfh_err))	// XXX
		//UVCPreview::uvc_preview_frame_callback()传递到此被回调 :
			strmh->user_cb(&strmh->frame, strmh->user_ptr);	// call user callback function  //调用用户的视频数据帧回调函数:
		else
			usleep(1000);//20180418 thread sleep one milliseconds

	}

	return NULL; // return value ignored
}

/** @internal
 * @brief Populate the fields of a frame to be handed to user code
 * must be called with stream cb lock held!
 将要将要交给用户代码的框架的字段填充
 必须调用流锁保持！
 */
 //UVC填充框架
void _uvc_populate_frame(uvc_stream_handle_t *strmh) {
	size_t alloc_size = strmh->cur_ctrl.dwMaxVideoFrameSize;
	uvc_frame_t *frame = &strmh->frame;
	uvc_frame_desc_t *frame_desc;

	/** @todo this stuff that hits the main config cache should really happen
	 * in start() so that only one thread hits these data. all of this stuff
	 * is going to be reopen_on_change anyway
	 这个东西，点击主配置缓存应该真的发生
     在start()使只有一个线程点击这些数据。所有这些东西
     要reopen_on_change无论如何
	 */

	frame_desc = uvc_find_frame_desc(strmh->devh, strmh->cur_ctrl.bFormatIndex,
			strmh->cur_ctrl.bFrameIndex);

	frame->frame_format = strmh->frame_format;

	frame->width = frame_desc->wWidth;
	frame->height = frame_desc->wHeight;
	// XXX set actual_bytes to zero when erro bits is on    //XXX 设置actual_bytes为零当错误位产生;
	frame->actual_bytes = LIKELY(!strmh->hold_bfh_err) ? strmh->hold_bytes : 0;

	switch (frame->frame_format) {
	case UVC_FRAME_FORMAT_YUYV:
		frame->step = frame->width * 2;
		break;
	case UVC_FRAME_FORMAT_MJPEG:
		frame->step = 0;
		break;
	default:
		frame->step = 0;
		break;
	}

	/* copy the image data from the hold buffer to the frame (unnecessary extra buf?)
	将图像数据从缓冲区复制到框架帧（不必要的额外缓冲区？）
	*/
	if (UNLIKELY(frame->data_bytes < strmh->hold_bytes)) {

	//realloc(*ptr,newsize)则对malloc()申请的内存进行大小的调整.
		frame->data = realloc(frame->data, strmh->hold_bytes);	// TODO add error handling when failed realloc  //添加错误处理当realloc失败
		frame->data_bytes = strmh->hold_bytes;
	}
	//memcpy(dest,src,size) 函数用于 把资源内存（src所指向的内存区域） 拷贝到目标内存（dest所指向的内存区域）
	memcpy(frame->data, strmh->holdbuf, strmh->hold_bytes/*frame->data_bytes*/);	// XXX 复制holdbuf缓冲区的图像帧数据到frame的data中;

	/** @todo set the frame time
	设置帧时间
	*/
}

/** Poll for a frame
 * @ingroup streaming
 *
 * @param devh UVC device
 * @param[out] frame Location to store pointer to captured frame (NULL on error)
 * @param timeout_us >0: Wait at most N microseconds; 0: Wait indefinitely; -1: return immediately
 */
 //该函数未被调用 :
uvc_error_t uvc_stream_get_frame(uvc_stream_handle_t *strmh,
		uvc_frame_t **frame, int32_t timeout_us) {
	time_t add_secs;
	time_t add_nsecs;
	struct timespec ts;
	struct timeval tv;

	if (UNLIKELY(!strmh->running))
		return UVC_ERROR_INVALID_PARAM;

	if (UNLIKELY(strmh->user_cb))
		return UVC_ERROR_CALLBACK_EXISTS;

	pthread_mutex_lock(&strmh->cb_mutex);
	{
		if (strmh->last_polled_seq < strmh->hold_seq) {
			_uvc_populate_frame(strmh);     //UVC填充数据帧frame 重要 ;
			*frame = &strmh->frame;
			strmh->last_polled_seq = strmh->hold_seq;
		} else if (timeout_us != -1) {
			if (!timeout_us) {
				pthread_cond_wait(&strmh->cb_cond, &strmh->cb_mutex);
			} else {
				add_secs = timeout_us / 1000000;
				add_nsecs = (timeout_us % 1000000) * 1000;
				ts.tv_sec = 0;
				ts.tv_nsec = 0;

#if _POSIX_TIMERS > 0
				clock_gettime(CLOCK_REALTIME, &ts);
#else
				gettimeofday(&tv, NULL);
				ts.tv_sec = tv.tv_sec;
				ts.tv_nsec = tv.tv_usec * 1000;
#endif

				ts.tv_sec += add_secs;
				ts.tv_nsec += add_nsecs;

				pthread_cond_timedwait(&strmh->cb_cond, &strmh->cb_mutex, &ts);
			}

			if (LIKELY(strmh->last_polled_seq < strmh->hold_seq)) {
				_uvc_populate_frame(strmh);     //UVC填充数据帧frame 重要 ;
				*frame = &strmh->frame;
				strmh->last_polled_seq = strmh->hold_seq;
			} else {
				*frame = NULL;
			}
		} else {
			*frame = NULL;
		}
	}
	pthread_mutex_unlock(&strmh->cb_mutex);

	return UVC_SUCCESS;
}

/** @brief Stop streaming video
 * @ingroup streaming
 *
 * Closes all streams, ends threads and cancels pollers
 *
 * @param devh UVC device
 */
void uvc_stop_streaming(uvc_device_handle_t *devh) {
	uvc_stream_handle_t *strmh, *strmh_tmp;

	UVC_ENTER();
	DL_FOREACH_SAFE(devh->streams, strmh, strmh_tmp)
	{
		uvc_stream_close(strmh);
	}
	UVC_EXIT_VOID();
}

/** @brief Stop stream.
 * @ingroup streaming
 *
 * Stops stream, ends threads and cancels pollers
 停流，两端螺纹和取消把
 *
 * @param devh UVC device
 */
uvc_error_t uvc_stream_stop(uvc_stream_handle_t *strmh) {

	int i;
	ENTER();

	if (!strmh) RETURN(UVC_SUCCESS, uvc_error_t);

	if (UNLIKELY(!strmh->running)) {
		UVC_EXIT(UVC_ERROR_INVALID_PARAM);
		RETURN(UVC_ERROR_INVALID_PARAM, uvc_error_t);
	}

	strmh->running = 0;

	pthread_mutex_lock(&strmh->cb_mutex);
	{
		for (i = 0; i < LIBUVC_NUM_TRANSFER_BUFS; i++) {
			if (strmh->transfers[i]) {
				int res = libusb_cancel_transfer(strmh->transfers[i]);
				if ((res < 0) && (res != LIBUSB_ERROR_NOT_FOUND)) {
					UVC_DEBUG("libusb_cancel_transfer failed");
					// XXX originally freed buffers and transfer here
					// but this could lead to crash in _uvc_callback
					// therefore we comment out these lines
					// and free these objects in _uvc_iso_callback when strmh->running is false
/*					free(strmh->transfers[i]->buffer);
					libusb_free_transfer(strmh->transfers[i]);
					strmh->transfers[i] = NULL; */
				}
			}
		}

		/* Wait for transfers to complete/cancel */
		for (; 1 ;) {

			for (i = 0; i < LIBUVC_NUM_TRANSFER_BUFS; i++) {
				if (strmh->transfers[i] != NULL)
					break;
			}
			if (i == LIBUVC_NUM_TRANSFER_BUFS)
				break;
			//20180418 thread sleep one milliseconds
			usleep(1000);
			pthread_cond_wait(&strmh->cb_cond, &strmh->cb_mutex);
		}
		// Kick the user thread awake
		pthread_cond_broadcast(&strmh->cb_cond);
	}
	pthread_mutex_unlock(&strmh->cb_mutex);

	/** @todo stop the actual stream, camera side? */

	if (strmh->user_cb) {
		/* wait for the thread to stop (triggered by LIBUSB_TRANSFER_CANCELLED transfer)
		等待线程停止（由libusb_transfer_cancelled转移触发）
		*/
		pthread_join(strmh->cb_thread, NULL);
	}

	RETURN(UVC_SUCCESS, uvc_error_t);
}

/** @brief Close stream.
 * @ingroup streaming
 *
 * Closes stream, frees handle and all streaming resources.
 关闭流，释放句柄和所有流媒体资源。
 *
 * @param strmh UVC stream handle
 */
void uvc_stream_close(uvc_stream_handle_t *strmh) {
	UVC_ENTER();

	if (!strmh) UVC_EXIT_VOID();

	if (strmh->running)
		uvc_stream_stop(strmh);

	uvc_release_if(strmh->devh, strmh->stream_if->bInterfaceNumber);

	if (strmh->frame.data) {
		free(strmh->frame.data);
		strmh->frame.data = NULL;
	}

	if (strmh->outbuf) {
		free(strmh->outbuf);
		strmh->outbuf = NULL;
	}
	if (strmh->holdbuf) {
		free(strmh->holdbuf);
		strmh->holdbuf = NULL;
	}

	pthread_cond_destroy(&strmh->cb_cond);
	pthread_mutex_destroy(&strmh->cb_mutex);

	DL_DELETE(strmh->devh->streams, strmh);
	free(strmh);

	UVC_EXIT_VOID();
}
