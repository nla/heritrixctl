package au.gov.nla.heritrixclient;

import java.io.PrintStream;

public class Heritrixctl {
    public static void main(String[] args) {
        System.exit(new Heritrixctl().exec(args));
    }

    private int exec(String[] args) {
        if (args.length == 0) {
            usage(System.out);
            return 0;
        }

        switch (args[0]) {
            case "run":
                run(args[0]);
                break;
            default:
                System.err.println("heritrixctl: Unknown command: " + args[0]);
                System.err.println("Try: 'heritrixctl help'");
                return 1;
        }
        return 0;
    }

    private void run(String cxml) {
    }

    private static void usage(PrintStream out) {
        out.println("Usage: heritrixctl [subcommand]\n" +
                "\n" +
                "Subcommands:" +
                "  run [crawler-beans.cxml]");
    }
}
