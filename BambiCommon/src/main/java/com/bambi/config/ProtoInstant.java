package com.bambi.config;

/**
 * 描述：
 *     通用配置类
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID          DATE              PERSON          REASON
 *  1       2022/12/7 10:47       Bambi           Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ProtoInstant {

    // 可读配置
    public static final short MAGIC_CODE = 0X86;
    public static final short VERSION_CODE = 0x01;

    /**
     * 客户端平台区分
     */
    public interface PlatForm{
        public static final int WINDOWS = 1;
        public static final int IOS = 2;
        public static final int ANDROID = 3;
        public static final int MAC = 4;
        public static final int WEB = 5;
        public static final int OTHER = 6;
    }

    /**
     * 返回代码枚举类
     */
    public enum ResultCodeEnum{
        SUCCESS(0,"SUCCESS"),
        AUTH_FAILED(1,"登录失败"),
        NO_TOKEN(2,"没有授权码(token)"),
        UNKNOW_ERROR(3,"未知错误 error");

        private Integer code;
        private String desc;

        ResultCodeEnum(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }
}
