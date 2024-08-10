import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private EditText nicknameEditText, addressEditText, portEditText, messageEditText, privateMessageEditText, privateNicknameEditText;
    private TextView chatTextView, statusTextView;
    private Button connectButton, sendButton, exportButton, sendPrivateButton;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE);
        }

        // Enable network operations on main thread (for simplicity)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        nicknameEditText = findViewById(R.id.nicknameEditText);
        addressEditText = findViewById(R.id.addressEditText);
        portEditText = findViewById(R.id.portEditText);
        messageEditText = findViewById(R.id.messageEditText);
        chatTextView = findViewById(R.id.chatTextView);
        statusTextView = findViewById(R.id.statusTextView);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        exportButton = findViewById(R.id.exportButton);
        sendPrivateButton = findViewById(R.id.sendPrivateButton);
        privateMessageEditText = findViewById(R.id.privateMessageEditText);
        privateNicknameEditText = findViewById(R.id.privateNicknameEditText);

        connectButton.setOnClickListener(this::connectToServer);
        sendButton.setOnClickListener(this::sendMessage);
        exportButton.setOnClickListener(this::exportChatHistory);
        sendPrivateButton.setOnClickListener(this::sendPrivateMessage);
    }

    private void connectToServer(View view) {
        String nickname = nicknameEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String port = portEditText.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                socket = new Socket(address, Integer.parseInt(port));
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() -> {
                    statusTextView.setText("已连接到服务器");
                    chatTextView.append("已连接到服务器\n");
                });

                // 接收消息
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    runOnUiThread(() -> chatTextView.append(finalMessage + "\n"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusTextView.setText("连接失败");
                    Toast.makeText(this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void sendMessage(View view) {
        String message = messageEditText.getText().toString().trim();
        if (!message.isEmpty() && socket != null) {
            String fullMessage = nicknameEditText.getText().toString() + ": " + message;
            out.println(fullMessage);
            chatTextView.append("我: " + message + "\n");
            messageEditText.setText("");
        }
    }

    private void sendPrivateMessage(View view) {
        String recipientNickname = privateNicknameEditText.getText().toString().trim();
        String privateMessage = privateMessageEditText.getText().toString().trim();

        if (!recipientNickname.isEmpty() && !privateMessage.isEmpty() && socket != null) {
            String fullMessage = "@" + recipientNickname + ": " + privateMessage;
            out.println(fullMessage);
            chatTextView.append("我 -> " + recipientNickname + ": " + privateMessage + "\n");
            privateMessageEditText.setText("");
        } else {
            Toast.makeText(this, "请输入私信内容和昵称", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportChatHistory(View view) {
        String chatHistory = chatTextView.getText().toString();
        if (!chatHistory.isEmpty()) {
            // Save chat history to a file
            try {
                File file = new File(getExternalFilesDir(null), "chat_history.txt");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(chatHistory.getBytes());
                fos.close();
                Toast.makeText(this, "聊天记录已导出到 " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导出聊天记录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "没有聊天记录可导出", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
