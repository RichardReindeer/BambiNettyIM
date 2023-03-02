package com.bambi.ezgate.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * 描述：
 *      <br><b>简易网关启动类</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/24 6:25    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@EntityScan(basePackages = "com.bambi.ezgate")
@ComponentScan("com.bambi.ezgate")
@SpringBootApplication
public class EzGateApplication {

    public static void main(String[] args) {
        SpringApplication.run(EzGateApplication.class, args);
    }

}
