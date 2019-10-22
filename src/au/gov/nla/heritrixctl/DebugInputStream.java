package au.gov.nla.heritrixctl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class DebugInputStream extends FilterInputStream {
    DebugInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        System.err.write(b, off, n);
        return n;
    }
}
