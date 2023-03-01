package com.bambi.exception;

/**
 * 描述：
 *      <br><b>自定义异常 格式不正确异常 </b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/24 8:50    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class InvalidFrameException extends Exception
{

    public InvalidFrameException(String s)
    {
        super(s);
    }
}
