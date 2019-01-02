package com.yeespec.microscope.master.service.client.socket.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by virgilyan on 15/7/4.
 * 连接MBox的所有命令
 */
public class MBOXConst {

    public static final boolean IS_DEBUG = true;

/*
    public static final String MBOX_HOST = "192.168.137.1";
    public static final int MBOX_PORT = 8888;
    public static final String MBOX_WIFI_PREFIX = "mbox";
    public static final String MBOX_WIFI_PASSWORD = "12345678";
    public static String MboxSSID = "";
*/

    public static final int MBOX_ORDER_CMD = 0;
  /*  public static final int MBOX_ORDER_P1 = 1;
    public static final int MBOX_ORDER_P2 = 2;

    public static final int MBOX_HEADER_INDEX = 0;
    public static final int MBOX_HEADER_CMD = 1;
    public static final int MBOX_HEADER_A = 2;
    public static final int MBOX_HEADER_B = 3;
    public static final int MBOX_HEADER_F = 4;
    public static final int MBOX_HEADER_R = 5;
    public static final int MBOX_HEADER_L = 6;
    public static final int MBOX_HEADER_LENGTH = 4 * 7;
    // MBox头长度
    public static final int MBOX_HEADER_EXTRAS_LENGTH = MBOX_WIFI_PREFIX.getBytes().length;
*/
    public static final Integer[] CMD_ORDER_END = new Integer[]{0, 0, 0};
    public static final Integer[] CMD_ORDER_CANCEL = new Integer[]{1, 0, 0};

    /**
     * 1、播放时间命令（已用）（完成）
     * 发送格式：15，0，0
     * 接受格式：Index，15，A，B，F，R，L
     * R：播放时间，单位：秒
     * F:31-0播放列表编号
     * A:23-16，（A>>16）&0xFF歌曲所在的目录编号
     * A:31-24，（A>>24）&0xFF重播状态
     * A:15-0 播放曲目编号
     * B:31 播放状态
     * B:30 暂停状态
     * B:29(暂时没用) 搜索状态
     * B:28-0曲目长度，单位：秒
     */
    public static final Integer[] CMD_PLAY_TIME = new Integer[]{15, 0, 0};

    /**
     * 2、文件总数命令（已用）（完成）
     * 发送格式：51，0，0
     * 接受格式：Index，51，A，B，F，R，L
     * R：文件总数
     * 文件总数中包含了“上级目录”。实际目录为：文件总数-1
     */
    public static final Integer[] CMD_TOTAL_FILE_NUMBER = new Integer[]{51, 0, 0};

    /**
     * 3、获得文件名命令（已用）（完成）
     * 发送格式：52，0，文件总数-1，0
     * 接受格式：（文件总数-1）* <Index，52，A，B，F，R，L + Data>
     * 接受帧数量：文件总数-1
     * A文件编号
     * L附加数据长度
     * Data文件名称的UNICODE编码
     * 目录名称的第一个UNICODE字符系文件类型，含义如下：
     * ‘0’-‘6’根目录
     * ‘L’磁盘或网络的LINK方式
     * ‘P’上级目录
     * ‘J’JPG图片
     * ‘B’BMP图片
     * ‘T’TXT文件
     * ‘D’子目录
     * ‘A’APE文件
     * ‘M’MP3文件
     * ‘K’FLAC文件
     * ‘S’ISO文件
     * 第二个字符开始系目录或文件名称。
     * 说明：
     * 1、	当文件类型系目录时（上一集/子目录），可以向主机发送33命令请求图片。
     * 2、	当文件类型为图片时（JPG/BMP）可以向主机发送33命令请求图片。
     */
    public static final Integer[] CMD_FILE_NAME = new Integer[]{52, 0, null};

    /**
     * 4、请求小图片命令---获取图标（已用）（完成）
     * 发送格式：33，文件编号，0
     * 接受格式： Index，33，A，B，F，R，L + Data
     * A：文件编号
     * B：图像高度，图像宽度为固定值 100
     * L：图像数据长度
     * R：997，传送中，999传送结束，998找不到图片。
     * 图像数据未经压缩，颜色深度8Bit，对应的调色板见附件。图像点阵排列从左到右，从上到下。
     */
    public static final Integer[] CMD_GET_SMALL_PICTURE = new Integer[]{33, null, 0};

    /**
     * 5、请求大图片命令---获取图片大图（已用）（完成）
     * 发送格式：60，文件编号，0
     * 接受格式： Index，60，A，B，F，R，L + Data
     * A：文件编号
     * B：图像高度，图像宽度为固定值 800
     * L：图像数据长度
     * R：997，传送中，999传送结束，998找不到图片。
     */
    public static final Integer[] CMD_GET_BIG_PICTURE = new Integer[]{60, null, 0};

    /**
     * 6、请求TXT文件内容命令（已用）（完成）
     * 发送格式：61，文件编号，0
     * 接受格式： Index，61，A，B，F，R，L + Data
     * L：图像数据长度
     * Data：文本内容（Unicode编码）
     */
    public static final Integer[] CMD_GET_TXT_CONTENT = new Integer[]{61, null, 0};

    /**
     * 7、设置播放列表命令（设置播放列表）（已用）（完成）
     * 发送格式：83，当前列表，播放列表
     * 如果“当前列表”“播放列表”设置为999，则不改变当前设置
     * 接受格式：Index，83，A，B，F，R，L + Data
     * A：当前列表
     * B：播放列表总数
     * R31:16 当前列表的歌曲总数
     * R15:0 正在播放的歌曲ID（0 - <歌曲总数-1>）
     */
    public static final Integer[] CMD_SET_UP_PLAY_LIST = new Integer[]{83, null, null};

    /**
     * 8、设置当前文件命令（已用）（完成）
     * 设置的文件用于标识那些目录或文件中的歌曲将会加入到列表。
     * 发送格式：55，文件编号，0
     * 接受格式：Index，55，A，B，F，R，0
     * R=998 出错：文件编号超出范围
     * R=999 成功。
     */
    public static final Integer[] CMD_SET_UP_CURRENT_FILE = new Integer[]{55, null, 0};

    /**
     * 9、歌曲检索命令（已用）（完成）
     * 发送格式：9，0，0
     * 接受格式：Index，0，A，B，F，R，0
     * A：加入的文件总数
     * B：当前列表的歌曲数目
     * 注：9命令的返回命令是0，通过检测命令0的完成，判断9命令完成。
     */
    public static final Integer[] CMD_MUSIC_RETRIEVAL = new Integer[]{9, 0, 0};

    /**
     * 10、确认加入歌曲命令（已用）（完成）
     * 发送格式：91，0，0
     * 接受格式：Index，91，A，B，F，R，0
     * A：新加入的歌曲总数。
     * 注：歌曲加入操作需要时间完成，可在91命令后，发出命令0，等待命令0完成后，说明加入操作已经完成。主机上的列表内容随即更新，但遥控器上的列表不会自动更新。
     */
    public static final Integer[] CMD_CONFIRM_ADD_MUSIC = new Integer[]{91, 0, 0};

    /**
     * 11、播放控制命令（已用）（完成）（完成）
     * 发送格式：11，歌曲编号，0
     * 接受格式：Index，11，歌曲编号，B，F，R，0
     * R=999 完成
     * R=998 出错：歌曲找不到
     */
    public static final Integer[] CMD_PLAY_SELECT_MUSIC = new Integer[]{11, null, 0};

    /**
     * 12、设置当前目录（已用）（完成）（完成）
     * 发送格式：54，P1，P2
     * 接受格式：Index，54，A，B，F，999，0 （可忽略）
     * P1=255 设置主机存储的目录为默认目录
     * P1=0 当处于非根目录时（此时有“上级目录”），设置上级目录为当前目录。当处于根目录时，设置第一个项目为当前目录。
     * P2=1 设置根目录为当前目录。
     * 注：根目录为E:\，外接硬盘下被安装在E:\下的某个目录，目录名是该硬盘的卷号。MBOX主机的共享目录为E:\SSD。网络上的共享目录在E:\NET下。
     */
    public static final Integer[] CMD_SET_UP_CURRENT_CATALOG = new Integer[]{54, null, null};

    /**
     * 13、设置播放列表对应目录为当前目录（已用）（完成）
     * 发送格式：53，P1，P2
     * 接受格式：Index，53，A，B，F，999，0（可忽略）
     * P1 为播放列表中目录的编号。
     * P2=0 针对当前列表操作。
     * P2=1 针对正在播放的列表操作。
     */
    public static final Integer[] CMD_SET_UP_PLAY_LIST_AS_CURRENT_CATALOG = new Integer[]{53, null, null};

    /**
     * 14、获得列表中的目录信息命令（已用）（完成）
     * 发送格式：82，P1，P2
     * P1 开始列表（通常是0）
     * P2 结束列表（通常是列表总数-1）（列表总数可通过命令83获得）
     * 接受格式：Index，82，A，B，0，0，0
     * A 列表ID（0 - <列表总数-1>）
     * B 该列表的歌曲总数
     */
    public static final Integer[] CMD_GET_LIST_INFORMATION = new Integer[]{82, null, null};

    /**
     * 15、获得列表的图片命令---歌单封面（已用）
     * 发送格式：81，P1，P2
     * P1 开始列表（通常是0）
     * P2 结束列表（通常是列表总数-1）（列表总数可通过命令83获得）
     * 接受格式：Index，81，A，B，0，0，L + Data
     * B 封面高度，宽度为固定值 112。
     * R=997 开始传送封面
     * R=999 传送结束
     * R=998 找不到封面
     * L 附加数据Data的长度
     * 注：Data格式同命令33
     */
    public static final Integer[] CMD_GET_LIST_PICTURE = new Integer[]{81, null, null};

    /**
     * 16、获取当前列表涉及的目录总数（已用）（完成）
     * 发送格式：21，0，0
     * 接受格式：Index，21，A，0，0，0，0
     * A 涉及的目录总数
     */
    public static final Integer[] CMD_CURRENT_CATALOG_QUANTITY = new Integer[]{21, 0, 0};

    /**
     * 17、获取当前列表的目录排列信息（已用）（完成）
     * 发送格式：23，P1，P2
     * P1 开始目录（通常是0）
     * P2 结束目录（通常是目录总数-1）（目录总数可通过命令21获得）
     * 接受格式：Index，23，A，B，F，R，L + Data
     * A 目录顺序号（0 - <目录总数-1>。
     * B 该目录的第一首歌曲ID。
     * F = 0
     * R 该目录涉及的歌曲总数
     * L 附加数据长度
     * Data 该目录的目录名称（Unicode）
     */
    public static final Integer[] CMD_GET_CURRENT_CATALOG_PERMUTATION_INFORMATION = new Integer[]{23, null, null};

    /**
     * 18、读取列表目录的封面---专辑封面（已用）
     * 发送格式：25，P1，P2
     * P1 开始目录（通常是0）
     * P2 结束目录（通常是目录总数-1）（目录总数可通过命令21获得）
     * 接受格式：Index，25，A，B，F，R，L + Data
     * A 目录顺序号（0 - <目录总数-1>。
     * B 图片高度，图片宽度为固定值76。
     * R =997 图片传送开始
     * R =999 图片传送结束
     * R =998 找不到图片
     * L 附加数据长度
     * Data 图片数据同命令33
     */
    public static final Integer[] CMD_GET_CATALOG_COVER = new Integer[]{25, 0, null};

    /**
     * 19、获得歌曲名称（已用）（完成）
     * 发送格式：32，P1，P2
     * P1 开始歌曲（通常是0）
     * P2 结束歌曲（通常是歌曲总数-1）（歌曲总数可通过命令23计算获得）
     * 接受格式：Index，32，A，B，F，R，L + Data
     * A 歌曲顺序号（0 - <歌曲总数-1>。
     * L 附加数据长度
     * Data 该歌曲的名称（Unicode）
     */
    public static final Integer[] CMD_GET_MUSIC_NAME = new Integer[]{32, null, null};

    /**
     * 20、获得歌曲长度和采样率信息（已用）（完成）
     * 发送格式：34，P1，P2
     * P1 开始歌曲（通常是0）
     * P2 结束歌曲（通常是歌曲总数-1）（歌曲总数可通过命令23计算获得）
     * 接受格式：Index，34，A，B，F，R，L + Data
     * A 歌曲顺序号（0 - <歌曲总数-1>。
     * R 歌曲长度，时间：秒
     * L 附加数据长度
     * Data 该歌曲采样率信息（Unicode）
     * 采样率信息的格式如：“44100 Hz 16 bits”
     */
    public static final Integer[] CMD_GET_MUSIC_INFORMATION = new Integer[]{34, 0, null};

    /**
     * 21、停止播放命令（已用）（完成）
     * 发送格式：10，0，0
     * 接受格式：Index，10，0，0，0，999，0
     */
    public static final Integer[] CMD_STOP_PLAY = new Integer[]{10, 0, 0};

    /**
     * 22、删除播放列表命令（已用）（完成）
     * 发送格式：87，P1，0
     * 删除编号为P1的列表，其后的列表编号减1。
     * 接受格式：Index，87，P1，B，0，0，0
     * B=0 P1超出范围
     * B=1 成功
     */
    public static final Integer[] CMD_DELETE_PLAY_LIST = new Integer[]{87, null, 0};

    /**
     * 23、增加播放列表命令（已用）（完成）
     * 发送格式：86，P1，0
     * 在P1位置增加新的列表，其后面的列表编号增加1。
     * 接受格式：Index，86，P1，B，0，0，0
     * B=0 P1超出范围
     * B=1 成功
     */
    public static final Integer[] CMD_ADD_PLAY_LIST = new Integer[]{86, null, 0};

    /**
     * 24、获得播放列表的目录图片（已用）
     * 发送格式：36，P1，0
     * P1 播放列表涉及的目录编号，P1=999当前的默认目录
     * 接受格式：Index，36，P1，B，F，R，L + Data
     * B 图片高度，宽度为固定值 100。
     * R = 997 开始传送
     * R = 999 传送结束
     * L 附加数据长度
     * Data 图片数据同命令33。
     */
    public static final Integer[] CMD_GET_CATALOG_PICTURE = new Integer[]{36, null, 0};

    /**
     * 25、播放控制命令（已用）（完成）
     * 发送格式：8，P1，P2
     * P1 = 4 上一曲
     * P1 = 3 播放/暂停
     * P1 = 5 下一曲
     * P2 无意义
     * 注：无返回数据。可通过15命令查看操作结果
     */
    public static final Integer[] CMD_PLAY_CONTROL_BY_PLAYED = new Integer[]{8, null, 0};
    // 播放/暂停
 /*   public static final int PLAY_CONTROL_BY_PLAYED_P2_PLAY_OR_PAUSE = 3;
    // 上一首
    public static final int PLAY_CONTROL_BY_PLAYED_P2_LAST = 4;
    // 下一首
    public static final int PLAY_CONTROL_BY_PLAYED_P2_NEXT = 5;
*/
    /**
     * 26、删除播放列表歌曲
     * 发送格式：27，P1，P2
     * P1 开始歌曲（通常是0）
     * P2 结束目录（通常是目录总数-1）（目录总数可通过命令21获得）
     * 接受格式：Index，27，，B，0，0，0
     * B=0 P1超出范围
     * B=1 成功
     */
    public static final Integer[] CMD_DELETE_PLAY_MUSIC = new Integer[]{27, null, null};

    /**
     * 27、播放循环控制
     * 发送格式：5，P1，0
     * P1 播放状态
     * 接受格式：Index，5，27，B，0，0，0
     * 0，无循环
     * 1，单曲循环
     * 2，列表循环
     */
    public static final Integer[] CMD_PLAYBACK_CONTROL = new Integer[]{5, null, 0};

    /**
     * 28、播放进度控制
     * 发送格式：16，P1，P2
     * P1 T/100
     * P2 T%100
     * 接受格式：Index，16，A，B，0，0，0
     */
    public static final Integer[] CMD_PLAYBACK_PROGRESS = new Integer[]{16, null, null};

    /**
     * 有效命令
     */
    public static final List<Integer> CALLBACK_EFFECTIVE_COMMAND = new ArrayList<>();
    /**
     * 列表命令
     */
    public static final List<Integer> CALLBACK_MESSAGE_LIST = new ArrayList<>();
    /**
     * 正常命令
     */
    public static final List<Integer> CALLBACK_NORMAL_COMMAND = new ArrayList<>();
    /**
     * 需要发送结束符的接口
     */
    public static final List<Integer> CALLBACK_NEED_END = new ArrayList<>();
    /**
     * 需要接收结束符的接口
     */
    public static final List<Integer> CALLBACK_BY_END = new ArrayList<>();

    static {
        // 普通命令
        CALLBACK_NORMAL_COMMAND.add(CMD_TOTAL_FILE_NUMBER[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_GET_SMALL_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_GET_BIG_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_GET_TXT_CONTENT[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_SET_UP_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_SET_UP_CURRENT_FILE[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_MUSIC_RETRIEVAL[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_CONFIRM_ADD_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_PLAY_SELECT_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_SET_UP_CURRENT_CATALOG[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_SET_UP_PLAY_LIST_AS_CURRENT_CATALOG[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_CURRENT_CATALOG_QUANTITY[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_STOP_PLAY[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_DELETE_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_ADD_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_PLAY_CONTROL_BY_PLAYED[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_DELETE_PLAY_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_PLAYBACK_CONTROL[MBOX_ORDER_CMD]);
        CALLBACK_NORMAL_COMMAND.add(CMD_PLAYBACK_PROGRESS[MBOX_ORDER_CMD]);

        // 列表命令
        CALLBACK_MESSAGE_LIST.add(CMD_FILE_NAME[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_LIST_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_LIST_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_CURRENT_CATALOG_PERMUTATION_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_CATALOG_COVER[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_MUSIC_NAME[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_MUSIC_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_MESSAGE_LIST.add(CMD_GET_CATALOG_PICTURE[MBOX_ORDER_CMD]);

        // 有效命令
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_ORDER_END[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_ORDER_CANCEL[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_PLAY_TIME[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_TOTAL_FILE_NUMBER[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_FILE_NAME[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_SMALL_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_BIG_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_TXT_CONTENT[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_SET_UP_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_SET_UP_CURRENT_FILE[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_MUSIC_RETRIEVAL[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_CONFIRM_ADD_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_PLAY_SELECT_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_SET_UP_CURRENT_CATALOG[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_SET_UP_PLAY_LIST_AS_CURRENT_CATALOG[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_LIST_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_LIST_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_CURRENT_CATALOG_QUANTITY[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_CURRENT_CATALOG_PERMUTATION_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_CATALOG_COVER[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_MUSIC_NAME[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_MUSIC_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_STOP_PLAY[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_DELETE_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_ADD_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_GET_CATALOG_PICTURE[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_PLAY_CONTROL_BY_PLAYED[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_DELETE_PLAY_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_PLAYBACK_CONTROL[MBOX_ORDER_CMD]);
        CALLBACK_EFFECTIVE_COMMAND.add(CMD_PLAYBACK_PROGRESS[MBOX_ORDER_CMD]);

        CALLBACK_NEED_END.add(CMD_TOTAL_FILE_NUMBER[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_FILE_NAME[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_MUSIC_RETRIEVAL[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_PLAY_SELECT_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_CONFIRM_ADD_MUSIC[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_SET_UP_PLAY_LIST[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_CURRENT_CATALOG_QUANTITY[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_GET_CURRENT_CATALOG_PERMUTATION_INFORMATION[MBOX_ORDER_CMD]);
        CALLBACK_NEED_END.add(CMD_GET_MUSIC_NAME[MBOX_ORDER_CMD]);

        CALLBACK_BY_END.add(CMD_MUSIC_RETRIEVAL[MBOX_ORDER_CMD]);
        CALLBACK_BY_END.add(CMD_CONFIRM_ADD_MUSIC[MBOX_ORDER_CMD]);
    }
}
//������F�[��nn����E���-^^ �&�����^�������������^,<��K^F-��-����^�g��	�����������F�[��nn����E���-^^ �&�����^�����F�[��- ��-�K^F-��-����^�g��	�����������F�[��nn����E���-^^ �&�����^�����F�[���-��<<K^F-��-����^�g��		�����������F�[� ����� �&�����^���-���n���� ,^F��K^F-��-����^�g��	�����������F�[� ����� �&�����^��n���� ^�K^F-��-����^�g��	�����������F�[� ����� �&�����^��^,<�ΐ������E�K^F-��-����^�g��	�����������F�[� ����� �&�����^������<������^,<��K^F-��-����^�g��	�����������F�[� ����� �&�����^���- n��^����^,<��K^F-��-����^�g��	�����������F�[� ����� �&�����^��<���,�������<�K^F-��-����^�g��	�����������F�[� ����� �&�����^���,��� �����-�, ���K^F-��-����^�g��	�����������F�[� ����� �&�����^������,��� �����-����^,���- �� n-�^��- K^F-��-����^�g��	�����������F�[� ����� �&�����^�����^,<�ΐ ^�K^F-��-����^�g��		�����������F�[�F��� �&�����^��^,<�ΐ������E�K^F-��-����^�g��	�����������F�[�F��� �&�����^���- n��^����^,<��K^F-��-����^�g��	�����	�