/**
 * 
 */
package sg.edu.ntu.jopinions.models;

/**Raised when a NaN is generated (usually division by 
 * zero), during matrix multiplication and normalization.
 * @author Amr
 *
 */
public class NaNException extends RuntimeException {

	private static final long serialVersionUID = 5372256491440765798L;

	public NaNException(String message) {
		super(message);
	}

}
