package rs2;


import java.io.*;
import java.net.Socket;

public class RSSocket
        implements Runnable
{

    public RSSocket(GameShell gameShell, Socket socket1)
        throws IOException
    {
        closed = false;
        isWriter = false;
        hasIOError = false;
        this.gameShell = gameShell;
        socket = socket1;
        socket.setSoTimeout(30000);
        socket.setTcpNoDelay(true);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public void close()
    {
        closed = true;
        try
        {
            if(inputStream != null)
                inputStream.close();
            if(outputStream != null)
                outputStream.close();
            if(socket != null)
                socket.close();
        }
        catch(IOException _ex)
        {
            System.out.println("Error closing stream");
        }
        isWriter = false;
        synchronized(this)
        {
            notify();
        }
        buffer = null;
    }

    public int read()
        throws IOException
    {
        if(closed)
            return 0;
        else
            return inputStream.read();
    }

    public int available()
        throws IOException
    {
        if(closed)
            return 0;
        else
            return inputStream.available();
    }

    public void flushInputStream(byte data[], int length)
        throws IOException
    {
        int offset = 0;//was parameter
        if(closed)
            return;
        int k;
        for(; length > 0; length -= k)
        {
            k = inputStream.read(data, offset, length);
            if(k <= 0)
                throw new IOException("EOF");
            offset += k;
        }

    }

    public void queueBytes(int i, byte abyte0[])
        throws IOException
    {
        if(closed)
            return;
        if(hasIOError)
        {
            hasIOError = false;
            throw new IOException("Error in writer thread");
        }
        if(buffer == null)
            buffer = new byte[5000];
        synchronized(this)
        {
            for(int l = 0; l < i; l++)
            {
                buffer[buffIndex] = abyte0[l];
                buffIndex = (buffIndex + 1) % 5000;
                if(buffIndex == (writeIndex + 4900) % 5000)
                    throw new IOException("buffer overflow");
            }

            if(!isWriter)
            {
                isWriter = true;
                gameShell.startRunnable(this, 3);
            }
            notify();
        }
    }

    public void run()
    {
        while(isWriter)
        {
            int length;
            int offset;
            synchronized(this)
            {
                if(buffIndex == writeIndex)
                    try
                    {
                        wait();
                    }
                    catch(InterruptedException _ex) { }
                if(!isWriter)
                    return;
                offset = writeIndex;
                if(buffIndex >= writeIndex)
                    length = buffIndex - writeIndex;
                else
                    length = 5000 - writeIndex;
            }
            if(length > 0)
            {
                try
                {
                    outputStream.write(buffer, offset, length);
                }
                catch(IOException _ex)
                {
                    hasIOError = true;
                }
                writeIndex = (writeIndex + length) % 5000;
                try
                {
                    if(buffIndex == writeIndex)
                        outputStream.flush();
                }
                catch(IOException _ex)
                {
                    hasIOError = true;
                }
            }
        }
    }

    public void printDebug()
    {
        System.out.println("dummy:" + closed);
        System.out.println("tcycl:" + writeIndex);
        System.out.println("tnum:" + buffIndex);
        System.out.println("writer:" + isWriter);
        System.out.println("ioerror:" + hasIOError);
        try
        {
            System.out.println("available:" + available());
        }
        catch(IOException _ex)
        {
        }
    }

    private InputStream inputStream;
    private OutputStream outputStream;
    private final Socket socket;
    private boolean closed;
    private final GameShell gameShell;
    private byte[] buffer;
    private int writeIndex;
    private int buffIndex;
    private boolean isWriter;
    private boolean hasIOError;
}
