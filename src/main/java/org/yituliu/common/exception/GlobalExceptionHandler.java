package org.yituliu.common.exception;



import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yituliu.common.utils.Result;


/**
 * 全局异常处理
 */

@RestControllerAdvice
public class GlobalExceptionHandler {



	@ResponseBody
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
		Result<Object> result = null;
		if(e instanceof ServiceException) {
			result = Result.failure(((ServiceException) e).getResultCode());
		}
		else {
			String message = e.getMessage();
			e.printStackTrace();
			if(message.contains("database")){
				int index = message.indexOf("###");
				if(index>-1){
					int endIndex = message.indexOf("###", index + 3);
					if(endIndex>-1){
						message = message.substring(0,endIndex);
					}
				}

			}
            result = Result.failure(500, message);
		}
		return result;
    }
	
}
