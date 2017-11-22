package dk.dbc.z3950IllProxy;

/**
 * Unittest utility methods
 */
public class Z3950TestUtils {

    /**
     * Prints the name of the method that calls this method.
     */
    public void printTestEntry() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[2].getMethodName());
    }
}
