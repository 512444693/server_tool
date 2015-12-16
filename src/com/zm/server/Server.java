package com.zm.server;

import com.zm.Field.CompareResult;
import com.zm.log.Log;
import com.zm.message.Message;
import com.zm.utils.BU;
import com.zm.utils.SU;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * Created by Administrator on 2015/12/13.
 */
public class Server implements Runnable {
    boolean TCP = true;

    ServerSocket serverSocket;
    Socket socket;

    DatagramSocket datagramSocket;
    DatagramPacket packet;

    byte[] rec = new byte[4096];
    int recLen = 0;

    JFrame frame;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenuItem saveMenuItem;
    JMenuItem openMenuItem;

    JPanel ctrlPanel;
    JLabel titleLabel;
    JTextField titleField;
    JButton titleButton;
    JLabel portLabel;
    JTextField portField;
    JButton startButton;
    ButtonGroup group;
    JRadioButton TCPButton;
    JRadioButton UDPButton;

    JPanel textPanel;
    JScrollPane recScrollPane;
    JScrollPane decodeScrollPane;
    JScrollPane sendScrollPane;
    JTextArea recArea;
    JTextArea decodeArea;
    JTextArea sendArea;

    public static void main(String[] args){
        new Server();
    }

    public Server() {
        frame = new JFrame("FAKE SERVER");
        //设置外观
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(frame);
        //设置大小和位置
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int screenHeight= screenSize.height;
        int screenWidth= screenSize.width;
        frame.setSize(screenWidth - 170, screenHeight - 180);
        frame.setLocation(screenWidth / 20, screenHeight / 20);

        menuBar = new JMenuBar();
        fileMenu = new JMenu("文件");
        saveMenuItem = new JMenuItem("保存");
        saveMenuItem.addActionListener(new SaveMenuListener());
        openMenuItem = new JMenuItem("打开");
        openMenuItem.addActionListener(new OpenMenuListener());
        fileMenu.add(saveMenuItem);
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        ctrlPanel = new JPanel();
        titleLabel = new JLabel("title");
        titleField = new JTextField("Fake Server");
        titleButton = new JButton("Set title");
        titleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setTitle(titleField.getText());
            }
        });
        portLabel = new JLabel("port");
        portField = new JTextField("8080");
        startButton  = new JButton("start");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port = Integer.parseInt(portField.getText());
                boolean tcp = TCPButton.isSelected();
                startServer(port, tcp);
                startButton.setEnabled(false);
                TCPButton.setEnabled(false);
                UDPButton.setEnabled(false);
                portField.setEnabled(false);
            }
        });
        group = new ButtonGroup();
        TCPButton = new JRadioButton("TCP", true);
        group.add(TCPButton);
        UDPButton = new JRadioButton("UDP", false);
        group.add(UDPButton);

        ctrlPanel.add(titleLabel);
        ctrlPanel.add(titleField);
        ctrlPanel.add(titleButton);
        ctrlPanel.add(portLabel);
        ctrlPanel.add(portField);
        ctrlPanel.add(TCPButton);
        ctrlPanel.add(UDPButton);
        ctrlPanel.add(startButton);

        textPanel = new JPanel();
        recArea = new JTextArea(35, 50);
        recArea.setLineWrap(true);
        recScrollPane = new JScrollPane(recArea);
        decodeArea = new JTextArea(35, 50);
        decodeArea.setEditable(false);
        //decodeArea.setBackground(new Color(121, 209, 255));
        decodeArea.setLineWrap(true);
        decodeScrollPane = new JScrollPane(decodeArea);
        sendArea = new JTextArea(35, 50);
        sendArea.setLineWrap(true);
        sendScrollPane = new JScrollPane(sendArea);
        textPanel.add(recScrollPane);
        textPanel.add(decodeScrollPane);
        textPanel.add(sendScrollPane);

        frame.getContentPane().add(BorderLayout.NORTH, ctrlPanel);
        frame.getContentPane().add(BorderLayout.CENTER, textPanel);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void process(byte[] data){
        decodeArea.setBackground(SU.randomColor());
        decodeArea.setText("");
        Message expect = null;
        Message fact = null;
        Message sendMsg = null;
        try{
            String recStr = recArea.getText().trim();
            if(!recStr.equals("")){
                expect = new Message(recStr);
                expect.encode();
                fact = new Message(recStr, data);
                fact.decode();
                decodeArea.setText(new Date().toString() + "\r\n\r\n" +fact.toString());
                CompareResult compareResult = expect.compare(fact);
                if(!compareResult.equal){
                    decodeArea.append(compareResult.msg);
                }
            }
            String sendStr = sendArea.getText().trim();
            if(!sendStr.equals("")){
                sendMsg = new Message(sendArea.getText());
                send(sendMsg.encode());
            }
        }catch (Exception e){
            //JOptionPane.showMessageDialog(null, e.getMessage());
            decodeArea.append(e.getMessage());//显示出解码失败信息
        }
    }

    public void startServer(int port, boolean TCP){
        if(serverSocket == null && datagramSocket ==null){
            this.TCP = TCP;
            try{
                if(TCP){
                    serverSocket = new ServerSocket(port);
                }else{
                    datagramSocket = new DatagramSocket(port);
                    packet = new DatagramPacket(rec, rec.length);
                }
                new Thread(this).start();
                decodeArea.setText("Listening...\r\n");
            }catch (IOException e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        while(true){
            try {
                if(TCP){
                    socket = serverSocket.accept();
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    recLen = in.read(rec);
                }else{
                    datagramSocket.receive(packet);
                    recLen = packet.getLength();
                }
                if(recLen != -1){
                    Log.rec(BU.subByte(rec, 0, recLen));
                    process(BU.subByte(rec, 0, recLen));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void send(byte[] data){
        try {
            if(TCP){
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                out.write(data);
                out.flush();
                out.close();
            }else {
                DatagramPacket send = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                datagramSocket.send(send);
            }
            Log.send(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class SaveMenuListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(frame);
            if(fileSave.getSelectedFile() != null)
                saveFile(fileSave.getSelectedFile());
        }
    }

    public class OpenMenuListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(frame);
            if(fileOpen.getSelectedFile() != null)
                openFile(fileOpen.getSelectedFile());
        }
    }

    public void saveFile(File file){
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.write(titleField.getText().trim().equals("")?" ":titleField.getText().trim()); writer.write("!@#$%^&*");
            writer.write(portField.getText().trim().equals("")?" ":portField.getText().trim()); writer.write("!@#$%^&*");
            writer.write(TCPButton.isSelected() + ""); writer.write("!@#$%^&*");
            writer.write(recArea.getText().trim().equals("")?" ":recArea.getText().trim()); writer.write("!@#$%^&*");
            writer.write(sendArea.getText().trim().equals("")?" ":sendArea.getText().trim());
            writer.close();
        } catch (Exception e) {
            decodeArea.setText(e.getMessage());
        }
    }

    public void openFile(File file){
        try {
            System.out.println("123");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            char[] data = new char[4096];
            int len = 0;
            String tmp = "";
            while ((len = reader.read(data, 0, 4096)) != -1)
                tmp += new String(data, 0, len);
            String[] strs = tmp.split("\\!\\@\\#\\$\\%\\^\\&\\*");
            if(strs.length != 5)
                return;
            titleField.setText(strs[0].trim());
            frame.setTitle(titleField.getText());
            portField.setText(strs[1].trim());
            boolean ifTCP = strs[2].equals("true")?true:false;
            TCPButton.setSelected(ifTCP);
            UDPButton.setSelected(!ifTCP);
            recArea.setText(strs[3].trim());
            sendArea.setText(strs[4].trim());
            reader.close();
        } catch (Exception e) {
            decodeArea.setText(e.getMessage());
        }
    }
}
