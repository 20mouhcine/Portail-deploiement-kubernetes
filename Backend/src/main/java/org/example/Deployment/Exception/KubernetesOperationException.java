package org.example.Deployment.Exception;

/**
 * @author pc
 **/
public class KubernetesOperationException extends RuntimeException {
    public KubernetesOperationException(String message) {
        super(message);
    }
}
