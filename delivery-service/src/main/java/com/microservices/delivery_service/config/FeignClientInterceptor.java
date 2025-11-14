package com.microservices.delivery_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        try{
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if(attributes!=null){
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

                if(authorizationHeader!=null && authorizationHeader.startsWith("Bearer ")){
                    log.debug("Forwarding authorizationHeader to Feign Client");
                    template.header(HttpHeaders.AUTHORIZATION,authorizationHeader);
                }else {
                    log.warn("Authorization header not found");
                }
            }
        }catch (Exception e){
            log.error("Error getting Authorization header from RequestContextHolder: {}",e.getMessage());
        }
    }
}
