package HttpMultipartMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import android.util.Log;

// http://blog.rafaelsanches.com/2011/01/29/upload-using-multipart-post-using-httpclient-in-android/

public class SimpleMultipartEntity implements HttpEntity {
	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final String TAG = SimpleMultipartEntity.class.getSimpleName();
	private String boundary = null;
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	boolean isSetLast = false;
	boolean isSetFirst = false;

	public SimpleMultipartEntity() {
		Log.d(TAG, "SimpleMultipartEntity");
		final StringBuffer buf = new StringBuffer();
		final Random rand = new Random();
		for (int i = 0; i < 30; i++) {
			buf.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
		}
		this.boundary = buf.toString();
	}

	public void writeFirstBoundaryIfNeeds(){
		Log.d(TAG, "writeFirstBoundaryIfNeeds");

		if(!isSetFirst){
			try {
				out.write(("--" + boundary + "\r\n").getBytes());
			} catch (final IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		isSetFirst = true;
	}

	public void writeLastBoundaryIfNeeds() {
		Log.d(TAG, "writeLastBoundaryIfNeeds");
		if(isSetLast){
			return ;
		}
		try {
			out.write(("\r\n--" + boundary + "--\r\n").getBytes());
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		isSetLast = true;
	}

	public void addPart(final String key, final String value) {
		addPart(key, value.getBytes());
	}
	public void addPart(final String key, final byte[] value) {
		Log.d(TAG, "addPart");
		writeFirstBoundaryIfNeeds();
		try {
//			out.write(("--" + boundary + "\r\n").getBytes());
			out.write(("Content-Disposition: form-data; name=\"" +key+"\"\r\n").getBytes());
			out.write("Content-Type: text/plain; charset=UTF-8\r\n".getBytes());
			out.write("Content-Transfer-Encoding: 8bit\r\n\r\n".getBytes());
			out.write(value);
			out.write(("\r\n--" + boundary + "\r\n").getBytes());
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public void addFile(final String key, final byte[] value, final String type) {
		writeFirstBoundaryIfNeeds();
		final String filename = key;
		try {
			final String contentType = "Content-Type: "+type+"\r\n";
			out.write(("Content-Disposition: form-data; name=\""+ key+"\"; filename=\"" + filename + "\"\r\n").getBytes());
			out.write(contentType.getBytes());
			out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());
			out.write(value);
			out.write(("\r\n--" + boundary + "\r\n").getBytes());
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	public void addPart(final String key, final String fileName, final InputStream fin){
		addPart(key, fileName, fin, "application/octet-stream");
	}

	// MSH: Add binary file
	public void addPart(final String key, final String fileName, final InputStream fin, String type){
//		writeFirstBoundaryIfNeeds();
		try {
			type = "Content-Type: "+type+"\r\n";
			out.write(("Content-Disposition: form-data; name=\""+ key+"\"; filename=\"" + fileName + "\"\r\n").getBytes());
			out.write(type.getBytes());
			out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());

			final byte[] tmp = new byte[4096];
			int l = 0;
			while ((l = fin.read(tmp)) != -1) {
				out.write(tmp, 0, l);
			}
			out.flush();
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			try {
				fin.close();
			} catch (final IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

    public void addPart(final String key, final File value) {
        try {
            addPart(key, value.getName(), new FileInputStream(value));
        } catch (final FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public long getContentLength() {
        writeLastBoundaryIfNeeds();
        return out.toByteArray().length;
    }

    @Override
    public Header getContentType() {
        return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

	@Override
	public void writeTo(final OutputStream outstream) throws IOException {
		outstream.write(out.toByteArray());
	}

	@Override
	public Header getContentEncoding() {
		return null;
	}

	@Override
	public void consumeContent() throws IOException, UnsupportedOperationException {
		Log.d(TAG, "consumeContent");
		if (isStreaming()) {
			throw new UnsupportedOperationException("Streaming entity does not implement #consumeContent()");
		}
	}

	@Override
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		Log.d(TAG, "getContent");
		return new ByteArrayInputStream(out.toByteArray());
	}

}