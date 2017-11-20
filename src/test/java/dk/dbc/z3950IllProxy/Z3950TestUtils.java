package dk.dbc.z3950IllProxy;

public class Z3950TestUtils {
    public void printTestEntry() {
        System.out.println("Running " + Thread.currentThread().getStackTrace()[2].getMethodName());
    }
}
