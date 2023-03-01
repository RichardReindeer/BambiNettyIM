package com.bambi.ezgate.swagger;

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 描述：
 *      <br><b>Swagger配置类</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 16:00    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    private static Logger logger = LoggerFactory.getLogger(SwaggerConfig.class);

    //swagger2的配置文件，这里可以配置swagger2的一些基本的内容，比如扫描的包等等
    @Bean
    public Docket createRestApi()
    {
        //为当前包路径,这个包指的是在哪些类中使用swagger2来测试
        Predicate<RequestHandler> selector = (Predicate<RequestHandler>) RequestHandlerSelectors
                .basePackage("com.bambi.ezgate.controller");
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(selector)
                .paths(PathSelectors.any())
                .build();
    }
    private ApiInfo apiInfo()
    {
        return new ApiInfoBuilder()
                //页面标题
                .title("短链接网关简单测试")
                //创建人
                .contact(new Contact("bambi","http://8.142.7.247:8954/","510614397@qq.com"))
                //版本号
                .version("1.0")
                .termsOfServiceUrl("http://localhost:8080/swagger-ui.html")   //描述
                .description("API 描述")
                .build();
    }
}
