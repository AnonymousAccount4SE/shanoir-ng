package org.shanoir.ng.shared.exception;

import org.slf4j.Logger;

/**
 * Microservice exception.
 * 
 * @author sloury
 *
 */
public class ShanoirPreclinicalException extends Exception {

	/**
	 * Serial version uid
	 */
	private static final long serialVersionUID = -1272303994850855360L;

	private int errorCode;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            message.
	 */
	public ShanoirPreclinicalException(final String message) {
		super(message);
	}

	public ShanoirPreclinicalException(Throwable cause, final String message) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param errorCode
	 *            error code.
	 */
	public ShanoirPreclinicalException(final int errorCode) {
		super();
		this.errorCode = errorCode;
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            message.
	 * @param errorCode
	 *            error code.
	 */
	public ShanoirPreclinicalException(final String message, final int errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * Log error and throw exception
	 * 
	 * @param logger
	 *            logger.
	 * @param message
	 *            message.
	 * @throws ShanoircenterException
	 */
	public static void logAndThrow(final Logger logger, final String message) throws ShanoirPreclinicalException {
		final ShanoirPreclinicalException e = new ShanoirPreclinicalException(message);
		logger.error(message, e);
		throw e;
	}

}
