package org.cds.main.blockchain.shell.config;

import static org.cds.main.blockchain.shell.util.AppConst.JSON_RPC_ALIAS_PATH;
import static org.cds.main.blockchain.shell.util.AppConst.JSON_RPC_PATH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cds.main.blockchain.shell.model.web.ClientLoginInfo;
import org.cds.main.blockchain.shell.model.web.WebConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;

/**
 * Filters web and rpc requests to ensure that
 * they are performed to the right port
 */
@WebFilter()
public class ModulePortFilter implements Filter {
	
	@Autowired
	HarmonyProperties harmonyProperties;
	
	private List<String> whiteList;
	private AntPathMatcher antPathMatcher = new AntPathMatcher();
	
    private Integer rpcPort;
    private Integer webPort;

    public ModulePortFilter(Integer rpcPort, Integer webPort) {
        this.rpcPort = rpcPort;
        this.webPort = webPort;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	whiteList = harmonyProperties.getConfig().getStringList("modules.rpc.ipWhiteList");
    	if(whiteList == null) {
    		whiteList = new ArrayList<>();
    	}
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(request instanceof HttpServletRequest) {
        	ClientLoginInfo clientLoginInfo = (ClientLoginInfo) ((HttpServletRequest) request).getSession().getAttribute(WebConstants.CLIENT_INFO);
            if (isRpcRequest((HttpServletRequest) request)) { // RPC request
                if (isRequestToWebPort(request)) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }else {
                	if(!validateIp((HttpServletRequest) request) && clientLoginInfo == null) {
                		((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                	}
                }
            } else { // Not RPC request
                if (isRequestToRpcPort(request)) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String requestUri = ((HttpServletRequest) request).getRequestURI();
            	if(!isPageStaticResource(requestUri) && !StringUtils.equals(requestUri, "/")) {
            		if(!StringUtils.contains(requestUri, "/websocket/") && clientLoginInfo == null) {
            			((HttpServletResponse) response).sendRedirect("/");
            			return;
            		}
            	}
            }
        }
        chain.doFilter(request, response);
    }
    
    private boolean isPageStaticResource(String url) {
    	return StringUtils.contains(url, "/images/")
    			|| StringUtils.contains(url, "/js/")
    			|| StringUtils.contains(url, "/pages/")
    			|| StringUtils.contains(url, "/styles/")
    			|| StringUtils.contains(url, "/vendor/")
    			|| StringUtils.contains(url, "/webjars/")
    			|| StringUtils.equals(url, "/index.html")
    			|| StringUtils.equals(url, "/user/login")
    			|| StringUtils.contains(url, "woff")
    			;
    }

    private boolean isRpcRequest(HttpServletRequest request) {
        return request.getRequestURI().equals(JSON_RPC_PATH) ||
                ("POST".equals(request.getMethod()) && request.getRequestURI().equals(JSON_RPC_ALIAS_PATH));
    }

    private boolean isRequestToRpcPort(ServletRequest request) {
        return rpcPort != null && request.getLocalPort() == rpcPort;
    }

    private boolean isRequestToWebPort(ServletRequest request) {
        return webPort != null && request.getLocalPort() == webPort;
    }

    @Override
    public void destroy() {

    }

    public static final Filter DUMMY = new Filter() {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            chain.doFilter(request, response);
        }
        @Override
        public void destroy() {
        }
    };
    
    private String getIPByHttpServletRequest(HttpServletRequest request) {
    	String ip = request.getHeader("x-forwarded-for");  
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
			ip = request.getHeader("Proxy-Client-IP");  
		}  
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
			ip = request.getHeader("WL-Proxy-Client-IP");  
		}  
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
			ip = request.getHeader("HTTP_CLIENT_IP");  
		}  
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
		}  
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
			ip = request.getRemoteAddr();  
		}
		return ip;
    }
    
    private boolean validateIp(HttpServletRequest request) {
    	String ip = getIPByHttpServletRequest(request);
    	if(StringUtils.equalsIgnoreCase("127.0.0.1", ip) || StringUtils.equalsIgnoreCase("localhost", ip)) {
    		return true;
    	}
    	for(String whiteIp:this.whiteList) {
    		if(antPathMatcher.match(whiteIp, ip)) {
    			return true;
    		}
    	}
    	return false;
    }
}
