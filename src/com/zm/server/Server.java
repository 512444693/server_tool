package com.zm.server;

import com.zm.Field.CompareResult;
import com.zm.log.Log;
import com.zm.message.Message;
import com.zm.message.RequestMessage;
import com.zm.utils.BU;
import com.zm.utils.SU;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 2015/12/13.
 */
public class Server implements Runnable {
    ConnectionType cntType = ConnectionType.TCP;

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
    JLabel portLabel;
    JTextField portField;
    JButton startButton;
    JButton stopButton;
    ButtonGroup group;
    JRadioButton TCPButton;
    JRadioButton LONGButton;
    JRadioButton UDPButton;
    JButton addMsgPanelButton;

    JCheckBox resWait;
    JLabel resWaitInfo;

    JPanel msgZone;
    JScrollPane msgScrollZone;

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
        frame.setSize(screenWidth, screenHeight - 480);
        frame.setLocation(0, screenHeight / 8);

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
        titleField = new JTextField("fake_", 20);
        titleField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }
            @Override
            public void focusLost(FocusEvent e) {
                frame.setTitle(titleField.getText());
            }
        });
        portLabel = new JLabel("port");
        portField = new JTextField("8080");
        startButton  = new JButton("start");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
                startButton.setEnabled(false);
                TCPButton.setEnabled(false);
                LONGButton.setEnabled(false);
                UDPButton.setEnabled(false);
                portField.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });
        stopButton = new JButton("stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(cntType == ConnectionType.UDP){
                    if(datagramSocket != null)
                        datagramSocket.close();
                }else{
                    try {
                        if(socket != null)
                            socket.close();
                        if(serverSocket != null)
                            serverSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                startButton.setEnabled(true);
                TCPButton.setEnabled(true);
                LONGButton.setEnabled(true);
                UDPButton.setEnabled(true);
                portField.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
        addMsgPanelButton = new JButton("add");
        addMsgPanelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msgZone.add(genMsgPanel("", ""));
                msgScrollZone.revalidate();
                msgScrollZone.repaint();
            }
        });
        group = new ButtonGroup();
        TCPButton = new JRadioButton("TCP", true);
        group.add(TCPButton);
        LONGButton = new JRadioButton("LONG", false);
        group.add(LONGButton);
        UDPButton = new JRadioButton("UDP", false);
        group.add(UDPButton);

        resWait = new JCheckBox("回包等待", false);

        resWaitInfo = new JLabel("                     ");

        ctrlPanel.add(titleLabel);
        ctrlPanel.add(titleField);
        ctrlPanel.add(portLabel);
        ctrlPanel.add(portField);
        ctrlPanel.add(TCPButton);
        ctrlPanel.add(LONGButton);
        ctrlPanel.add(UDPButton);
        ctrlPanel.add(startButton);
        ctrlPanel.add(stopButton);
        ctrlPanel.add(addMsgPanelButton);
        ctrlPanel.add(resWait);
        ctrlPanel.add(resWaitInfo);

        msgZone = new JPanel();
        BoxLayout boxLayout = new BoxLayout(msgZone, BoxLayout.Y_AXIS);
        msgZone.setLayout(boxLayout);
        msgZone.add(genMsgPanel("", ""));

        msgScrollZone = new JScrollPane(msgZone);

        frame.getContentPane().add(BorderLayout.NORTH, ctrlPanel);
        frame.getContentPane().add(BorderLayout.CENTER, msgScrollZone);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public JPanel genMsgPanel(String recAreaText, String sendAreaText){
        JPanel textPanel;
        JScrollPane recScrollPane;
        JScrollPane decodeScrollPane;
        JScrollPane sendScrollPane;
        JScrollPane encodeScrollPane;
        JTextArea recArea;
        JTextArea decodeArea;
        JTextArea sendArea;
        JTextArea encodeArea;
        int height = 15, width = 49;
        textPanel = new JPanel();
        recArea = new JTextArea(height, width);
        recArea.setText(recAreaText);
        recScrollPane = new JScrollPane(recArea);
        recScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        recScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        decodeArea = new JTextArea(height, width);
        decodeArea.setEditable(false);
        decodeScrollPane = new JScrollPane(decodeArea);
        decodeScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        decodeScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sendArea = new JTextArea(height, width);
        sendArea.setText(sendAreaText);
        sendScrollPane = new JScrollPane(sendArea);
        sendScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sendScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        encodeArea = new JTextArea(height, width);
        encodeArea.setEditable(false);
        encodeScrollPane = new JScrollPane(encodeArea);
        encodeScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        encodeScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textPanel.add(recScrollPane);
        textPanel.add(decodeScrollPane);
        textPanel.add(sendScrollPane);
        textPanel.add(encodeScrollPane);
        return textPanel;
    }

    public JTextArea getTextAreaFromMsgZone(int x, int y){
        if(x > getMsgLength() || x < 0)
            return null;
        if(y < 0 || y > 3)
            return null;
        JPanel msgPanel = (JPanel)msgZone.getComponent(x);
        JScrollPane msgScrollPane = (JScrollPane)msgPanel.getComponent(y);
        return (JTextArea)msgScrollPane.getViewport().getView();
    }

    public int getMsgLength(){
        return msgZone.getComponents().length;
    }


    public void process(byte[] data){
        if(cntType == ConnectionType.LONG){//报活
            if(BU.bytes2Hex(data).matches("^0{92}$")){
                send(data);
                return;
            }
            //另一种长连接
            if(BU.bytes2Hex(data).equals("ffff000000000000000000000000000000000000ffffffffffffffffffffffff00000000")){
                send(BU.hex2Bytes("fffe000000000000000000000000000000000000ffffffffffffffffffffffff00000000"));
                return;
            }
        }
        Log.rec(data);
        Color color = SU.randomColor();
        Message expect = null;
        Message fact = null;
        Message sendMsg = null;
        boolean find = false;
        for(int i = 0; i < getMsgLength(); i++){
            JTextArea recArea = getTextAreaFromMsgZone(i, 0);
            JTextArea decodeArea = getTextAreaFromMsgZone(i, 1);
            JTextArea sendArea = getTextAreaFromMsgZone(i ,2);
            JTextArea encodeArea = getTextAreaFromMsgZone(i ,3);
            try{
                String recStr = recArea.getText().trim();
                if(!recStr.equals("")){
                    expect = new Message(recStr);
                    expect.encode();
                    fact = new Message(recStr, data);
                    try{//尝试解码，用来比较cmdid
                        fact.decode();
                    }catch (Exception e){}
                    if(fact.getCmdID() != expect.getCmdID())
                        continue;
                    find = true;
                    decodeArea.setBackground(color);
                    decodeArea.setText("");
                    fact = new Message(recStr, data);
                    fact.decode();//正式解码,需要捕获解码异常
                    RequestMessage.registerAsReqMsg(fact);
                    decodeArea.setText(new Date().toString() + "\r\n\r\n" + fact.toString());
                    if(fact.dataCntLeftToDecode() > 0)
                        decodeArea.append("还剩" + fact.dataCntLeftToDecode() + "字节数据没有解码" + "\r\n");
                    CompareResult compareResult = expect.compare(fact);
                    if(!compareResult.equal){
                        decodeArea.append(compareResult.msg);
                    }

                    while(resWait.isSelected()){
                        resWaitInfo.setBorder(new LineBorder(new Color(255, 0, 0)));
                        resWaitInfo.setText("回包等待中,不处理任何包");
                        Thread.sleep(1000);
                    }
                    resWaitInfo.setText("                     ");
                    resWaitInfo.setBorder(null);

                    if(!sendArea.getText().trim().equals("")){
                        sendMsg = new Message(sendArea.getText().trim());
                        byte[] sendData = sendMsg.encode();
                        send(sendData);
                        encodeArea.setBackground(color);
                        encodeArea.setText(new Date().toString() + "\r\n\r\n" + sendMsg.toString());
                        Log.send(sendData);
                    }
                    RequestMessage.clearReqMsg();
                }
            }catch (Exception e){
                decodeArea.append(e.getMessage());//显示出解码失败信息
            }
            break;
        }
        if(!find) {
            getTextAreaFromMsgZone(0, 1).setBackground(color);
            getTextAreaFromMsgZone(0, 1).setText(new Date().toString());
            getTextAreaFromMsgZone(0, 1).append("\r\n没有找到与之相同的cmdid");
            if(fact != null)
                getTextAreaFromMsgZone(0, 1).append("\r\n实际收到的cmdid可能为 " + fact.getCmdID());
        }
        if(cntType == ConnectionType.TCP){//短连接，无论怎样最后关闭连接则关闭连接
            try {
                if(socket != null)
                    socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }


    public void startServer(){
        int port = Integer.parseInt(portField.getText());
        if(TCPButton.isSelected())
            cntType = ConnectionType.TCP;
        else if(LONGButton.isSelected())
            cntType = ConnectionType.LONG;
        else cntType = ConnectionType.UDP;

        try{
            if(cntType == ConnectionType.UDP){
                datagramSocket = new DatagramSocket(port);
                packet = new DatagramPacket(rec, rec.length);
            }else{
                serverSocket = new ServerSocket(port);
            }
            new Thread(this).start();
            getTextAreaFromMsgZone(0, 1).setText("Listening...\r\n");
        }catch (IOException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

    }

    @Override
    public void run() {
        try {
            if(cntType == ConnectionType.LONG)
                socket = serverSocket.accept();//阻塞

            while(true) {
                if (cntType == ConnectionType.UDP) {
                    datagramSocket.receive(packet);//阻塞
                    recLen = packet.getLength();
                } else {
                    if (cntType == ConnectionType.TCP)
                        socket = serverSocket.accept();//阻塞
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    recLen = in.read(rec);//阻塞
                }
                if (recLen != -1) {
                    process(BU.subByte(rec, 0, recLen));
                }else{//如果为-1则为视连接被对方关闭
                    if(socket != null)
                        socket.close();
                    if(cntType == ConnectionType.LONG)
                        socket = serverSocket.accept();//阻塞
                }
            }
        } catch (IOException e) {
            getTextAreaFromMsgZone(0, 1).setText("停止监听\r\n");
        }catch (Exception ec){
            getTextAreaFromMsgZone(0, 1).setText("oops 程序发生错误，请stop后重新start\r\n");
            ec.printStackTrace();
        }
    }

    public void send(byte[] data){
        try {
            if(cntType == ConnectionType.UDP){
                DatagramPacket send = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                datagramSocket.send(send);
            }else {
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                out.write(data);
                out.flush();
                if(cntType == ConnectionType.TCP)
                    out.close();
            }
            //Log.send(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class SaveMenuListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.setSelectedFile(new File(fileSave.getCurrentDirectory().getPath() + "\\" + titleField.getText() + ".mm"));
            if(fileSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
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
            if(TCPButton.isSelected())//兼容以前版本
                writer.write("true");
            else if(UDPButton.isSelected())
                writer.write("false");
            else writer.write("long");
            writer.write("!@#$%^&*");
            for(int i = 0; i < getMsgLength(); i++){
                JTextArea recArea = getTextAreaFromMsgZone(i, 0);
                JTextArea sendArea = getTextAreaFromMsgZone(i ,2);
                writer.write(recArea.getText().trim().equals("")?" ":recArea.getText().trim()); writer.write("!@#$%^&*");
                writer.write(sendArea.getText().trim().equals("")?" ":sendArea.getText().trim());writer.write("!@#$%^&*");
            }
            writer.close();
        } catch (Exception e) {
            getTextAreaFromMsgZone(0, 1).setText(e.getMessage());
        }
    }

    public void openFile(File file){
        try {
            for(int i = getMsgLength() - 1; i > 0; i--){
                msgZone.remove(i);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            char[] data = new char[4096];
            int len = 0;
            String tmp = "";
            while ((len = reader.read(data, 0, 4096)) != -1)
                tmp += new String(data, 0, len);
            String[] strs = tmp.split("\\!\\@\\#\\$\\%\\^\\&\\*");
            if(strs.length < 5)
                return;
            titleField.setText(strs[0].trim());
            frame.setTitle(titleField.getText());
            portField.setText(strs[1].trim());
            //兼容以前版本
            TCPButton.setSelected(strs[2].equals("true"));
            UDPButton.setSelected(strs[2].equals("false"));
            LONGButton.setSelected(strs[2].equals("long"));
            getTextAreaFromMsgZone(0, 0).setText(strs[3].trim());
            getTextAreaFromMsgZone(0, 2).setText(strs[4].trim());
            for(int i = 5; i < strs.length; i+=2){
                msgZone.add(genMsgPanel(strs[i], strs[i+1]));
            }
            msgScrollZone.revalidate();
            msgScrollZone.repaint();
            reader.close();
        } catch (Exception e) {
            getTextAreaFromMsgZone(0, 1).setText(e.getMessage());
        }
    }
}
