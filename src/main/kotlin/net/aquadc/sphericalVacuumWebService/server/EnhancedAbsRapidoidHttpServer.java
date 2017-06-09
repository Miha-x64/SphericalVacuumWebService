package net.aquadc.sphericalVacuumWebService.server;

import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.impl.lowlevel.HttpIO;
import org.rapidoid.net.abstracts.Channel;

public abstract class EnhancedAbsRapidoidHttpServer extends AbstractHttpServer {

    protected HttpStatus ok(Channel ctx, boolean isKeepAlive, byte[] body, MediaType contentType) {
        return ok(ctx, isKeepAlive, body, 0, body.length, contentType);
    }

    protected HttpStatus ok(Channel ctx, boolean isKeepAlive, byte[] body, int offset, int length, MediaType contentType) {
        startResponse(ctx, isKeepAlive);
        writeBody(ctx, body, offset, length, contentType);
        return HttpStatus.DONE;
    }

    protected void writeBody(Channel ctx, byte[] body, MediaType contentType) {
        writeBody(ctx, body, 0, body.length, contentType);
    }

    protected void writeBody(Channel ctx, byte[] body, int offset, int length, MediaType contentType) {
        writeContentTypeHeader(ctx, contentType);
        HttpIO.INSTANCE.writeContentLengthHeader(ctx, body.length);

        ctx.write(CR_LF);

        ctx.write(body, offset, length);
    }

    private void writeContentTypeHeader(Channel ctx, MediaType contentType) {
        ctx.write(CONTENT_TYPE_TXT);
        ctx.write(contentType.getBytes());
        ctx.write(CR_LF);
    }

}
