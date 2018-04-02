package socket.step.mine.com.cellserverdemo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private TextView editText_1;
    private TextView editText_2;
    private ServerSocket serverSocket = null;
    StringBuffer stringBuffer = new StringBuffer();

    private InputStream inputStream;

    public Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case 1:
                    editText_1.setText(msg.obj.toString());
                    break;
                case 2:
                    editText_2.setText(msg.obj.toString());
                    stringBuffer.setLength(0);
                    break;

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_1 = findViewById(R.id.status);
        editText_2 = findViewById(R.id.content);

        receiveData();
    }

    /*
    服务器端接收数据
    需要注意以下一点：
    服务器端应该是多线程的，因为一个服务器可能会有多个客户端连接在服务器上；
    */
    public void receiveData(){

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                /*指明服务器端的端口号*/
                try {
                    serverSocket = new ServerSocket(8000);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                GetIpAddress.getLocalIpAddress(serverSocket);

                Message message_1 = handler.obtainMessage();
                message_1.what = 1;
                message_1.obj = "IP:" + GetIpAddress.getIP() + "\nPORT: " + GetIpAddress.getPort();
                handler.sendMessage(message_1);

                while (true){
                    Log.e("server", "receiveData --> loop" + Thread.currentThread().getName());
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        inputStream  = socket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("server", "receiveData --> loop error");
                    }
                    Log.e("server", "out receiveData --> loop" + Thread.currentThread().getName());
                    ServerThread serverThread = new ServerThread(socket,inputStream);
                    serverThread.setName(System.currentTimeMillis() + "");
                    serverThread.start();

                }
            }
        };
        thread.start();

    }

    class ServerThread extends Thread{

        private Socket socket;
        private InputStream inputStream;
        private StringBuffer stringBuffer = MainActivity.this.stringBuffer;


        public ServerThread(Socket socket,InputStream inputStream){
            this.socket = socket;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            int len;
            byte[] bytes = new byte[1024 * 4];
            try {
                int available = inputStream.available();
                //在这里需要明白一下什么时候其会等于 -1，其在输入流关闭时才会等于 -1，
                //并不是数据读完了，再去读才会等于-1，数据读完了，最结果也就是读不到数据为0而已；
                while ((len = inputStream.read(bytes)) != -1) {
                        Log.e("server", "len = " + len + ", content = " + new String(bytes, 0, len) + ", thread = " + Thread.currentThread().getName() + "\n count = " + inputStream.available());
                        System.out.println("gao -------- red --------------");
                        Message message_2 = handler.obtainMessage();
                        message_2.what = 2;
                        message_2.obj = "From:" + this.socket.getInetAddress().getHostAddress() + "--> \n" + new String(bytes, 0, len);
                        handler.sendMessage(message_2);
                }
                Log.e("server", "ou thread = " + Thread.currentThread().getName());
                //当这个异常发生时，说明客户端那边的连接已经断开
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    inputStream.close();
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }

        }
    }


    /*当按返回键时，关闭相应的socket资源*/
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}