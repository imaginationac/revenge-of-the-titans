package com.shavenpuppy.jglib.jpeg;

class JPEGError {

	public static final String ERROR_NOT_IMPLEMENTED = "Not Implemented";
	public static final String ERROR_INVALID_IMAGE = "Invalid Image";
	public static final String ERROR_IO = "I/O error";
	
	public static void error(String errorInvalidImage, Object object, String msg)  throws RuntimeException {
		throw new RuntimeException(errorInvalidImage+" "+object+" "+msg);
	}
	public static void error(String errorInvalidImage) throws RuntimeException {
		throw new RuntimeException(errorInvalidImage);
	}
	public static void error(int code) throws RuntimeException {
		throw new RuntimeException("Error code "+code);
	}

}
