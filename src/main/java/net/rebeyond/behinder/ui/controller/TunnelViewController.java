// 
// Decompiled by Procyon v0.5.36
// 

package net.rebeyond.behinder.ui.controller;

import net.rebeyond.behinder.utils.CipherUtils;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import javafx.event.ActionEvent;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.net.Socket;
import java.util.Iterator;
import javafx.application.Platform;
import java.io.IOException;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Toggle;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ToggleGroup;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.util.List;
import org.json.JSONObject;
import net.rebeyond.behinder.core.ShellService;
import javafx.scene.control.TextField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class TunnelViewController
{
    @FXML
    private Button createPortMapBtn;
    @FXML
    private Button createSocksBtn;
    @FXML
    private Label portMapListenIPLabel;
    @FXML
    private Label portMapListenPortLabel;
    @FXML
    private Label portMapDescLabel;
    @FXML
    private Label socksListenIPLabel;
    @FXML
    private Label socksListenPortLabel;
    @FXML
    private Label socksDescLabel;
    @FXML
    private TextArea tunnelLogTextarea;
    @FXML
    private RadioButton portmapVPSRadio;
    @FXML
    private RadioButton portmapHTTPRadio;
    @FXML
    private RadioButton socksVPSRadio;
    @FXML
    private RadioButton socksHTTPRadio;
    @FXML
    private TextField portMapTargetIPText;
    @FXML
    private TextField portMapTargetPortText;
    @FXML
    private TextField portMapIPText;
    @FXML
    private TextField portMapPortText;
    @FXML
    private TextField socksIPText;
    @FXML
    private TextField socksPortText;
    private ShellService currentShellService;
    private JSONObject shellEntity;
    private List<Thread> workList;
    private List<Thread> localList;
    private Label statusLabel;
    private ProxyUtils proxyUtils;
    private ServerSocket localPortMapSocket;

    public TunnelViewController() {
        this.localList = new ArrayList<Thread>();
    }

    public void init(final ShellService shellService, final List<Thread> workList, final Label statusLabel) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.getShellEntity();
        this.workList = workList;
        this.statusLabel = statusLabel;
        this.initTunnelView();
    }

    private void initTunnelView() {
        final ToggleGroup portmapTypeGroup = new ToggleGroup();
        this.portmapVPSRadio.setToggleGroup(portmapTypeGroup);
        this.portmapHTTPRadio.setToggleGroup(portmapTypeGroup);
        this.portmapVPSRadio.setUserData((Object)"remote");
        this.portmapHTTPRadio.setUserData((Object)"local");
        final ToggleGroup socksTypeGroup = new ToggleGroup();
        this.socksVPSRadio.setToggleGroup(socksTypeGroup);
        this.socksHTTPRadio.setToggleGroup(socksTypeGroup);
        this.socksVPSRadio.setUserData((Object)"remote");
        this.socksHTTPRadio.setUserData((Object)"local");
        portmapTypeGroup.selectedToggleProperty().addListener((ChangeListener)new ChangeListener<Toggle>() {
            public void changed(final ObservableValue<? extends Toggle> ov, final Toggle oldToggle, final Toggle newToggle) {
                if (portmapTypeGroup.getSelectedToggle() != null) {
                    final String portMapType = newToggle.getUserData().toString();
                    if (portMapType.equals("local")) {
                        TunnelViewController.this.portMapDescLabel.setText("*提供基于HTTP隧道的单端口映射，将远程目标内网端口映射到本地，适用于目标不能出网的情况。");
                        TunnelViewController.this.portMapListenIPLabel.setText("本地监听IP地址：");
                        TunnelViewController.this.portMapListenPortLabel.setText("本地监听端口：");
                        TunnelViewController.this.portMapIPText.setText("0.0.0.0");
                    }
                    else if (portMapType.equals("remote")) {
                        TunnelViewController.this.portMapDescLabel.setText("*提供基于VPS中转的单端口映射，将远程目标内网端口映射到VPS，目标机器需要能出网。");
                        TunnelViewController.this.portMapListenIPLabel.setText("VPS监听IP地址：");
                        TunnelViewController.this.portMapListenPortLabel.setText("VPS监听端口：");
                        TunnelViewController.this.portMapIPText.setText("8.8.8.8");
                    }
                }
            }
        });
        this.portMapListenIPLabel.setText("VPS监听IP地址：");
        this.portMapListenPortLabel.setText("VPS监听端口：");
        socksTypeGroup.selectedToggleProperty().addListener((ChangeListener)new ChangeListener<Toggle>() {
            public void changed(final ObservableValue<? extends Toggle> ov, final Toggle oldToggle, final Toggle newToggle) {
                if (portmapTypeGroup.getSelectedToggle() != null) {
                    final String portMapType = newToggle.getUserData().toString();
                    if (portMapType.equals("local")) {
                        TunnelViewController.this.socksDescLabel.setText("*提供基于HTTP隧道的全局socks代理，将远程目标内网的socks代理服务开到本地，适用于目标不能出网的情况。");
                        TunnelViewController.this.socksListenIPLabel.setText("本地监听IP地址：");
                        TunnelViewController.this.socksListenPortLabel.setText("本地监听端口：");
                    }
                    else if (portMapType.equals("remote")) {
                        TunnelViewController.this.socksDescLabel.setText("*提供基于VPS中转的全局socks代理，将远程目标内网的socks代理服务开到外网VPS，目标机器需要能出网。");
                        TunnelViewController.this.socksListenIPLabel.setText("VPS监听IP地址：");
                        TunnelViewController.this.socksListenPortLabel.setText("VPS监听端口：");
                    }
                }
            }
        });
        this.createPortMapBtn.setOnAction(event -> {
            if (this.createPortMapBtn.getText().equals("开启")) {
                final RadioButton currentTypeRadio = (RadioButton)portmapTypeGroup.getSelectedToggle();
                if (currentTypeRadio.getUserData().toString().equals("local")) {
                    this.createLocalPortMap();
                }
                else if (currentTypeRadio.getUserData().toString().equals("remote")) {
                    this.createRemotePortMap();
                }
            }
            else {
                final RadioButton currentTypeRadio = (RadioButton)portmapTypeGroup.getSelectedToggle();
                if (currentTypeRadio.getUserData().toString().equals("local")) {
                    this.stoplocalPortMap();
                }
                else if (currentTypeRadio.getUserData().toString().equals("remote")) {
                    this.stopRemotePortMap();
                }
            }
        });
        this.createSocksBtn.setOnAction(event -> {
            if (this.createSocksBtn.getText().equals("开启")) {
                final RadioButton currentTypeRadio = (RadioButton)socksTypeGroup.getSelectedToggle();
                if (currentTypeRadio.getUserData().toString().equals("local")) {
                    this.createLocalSocks();
                }
                else if (currentTypeRadio.getUserData().toString().equals("remote")) {
                    this.createRemoteSocks();
                }
            }
            else {
                final RadioButton currentTypeRadio = (RadioButton)socksTypeGroup.getSelectedToggle();
                if (currentTypeRadio.getUserData().toString().equals("local")) {
                    this.stopLocalSocks();
                }
                else if (currentTypeRadio.getUserData().toString().equals("remote")) {
                    this.stopRemoteSocks();
                }
            }
        });
    }

    private void createLocalPortMap() {
        //
        // This method could not be decompiled.
        //
        // Original Bytecode:
        //
        //     1: getfield        net/rebeyond/behinder/ui/controller/TunnelViewController.createPortMapBtn:Ljavafx/scene/control/Button;
        //     4: ldc             "关闭"
        //     6: invokevirtual   javafx/scene/control/Button.setText:(Ljava/lang/String;)V
        //     9: aload_0         /* this */
        //    10: getfield        net/rebeyond/behinder/ui/controller/TunnelViewController.portMapTargetIPText:Ljavafx/scene/control/TextField;
        //    13: invokevirtual   javafx/scene/control/TextField.getText:()Ljava/lang/String;
        //    16: astore_1        /* targetIP */
        //    17: aload_0         /* this */
        //    18: getfield        net/rebeyond/behinder/ui/controller/TunnelViewController.portMapTargetPortText:Ljavafx/scene/control/TextField;
        //    21: invokevirtual   javafx/scene/control/TextField.getText:()Ljava/lang/String;
        //    24: astore_2        /* targetPort */
        //    25: aload_0         /* this */
        //    26: aload_1         /* targetIP */
        //    27: aload_2         /* targetPort */
        //    28: invokedynamic   BootstrapMethod #2, run:(Lnet/rebeyond/behinder/ui/controller/TunnelViewController;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Runnable;
        //    33: astore_3        /* creater */
        //    34: new             Ljava/lang/Thread;
        //    37: dup
        //    38: aload_3         /* creater */
        //    39: invokespecial   java/lang/Thread.<init>:(Ljava/lang/Runnable;)V
        //    42: astore          worker
        //    44: aload_0         /* this */
        //    45: getfield        net/rebeyond/behinder/ui/controller/TunnelViewController.workList:Ljava/util/List;
        //    48: aload           worker
        //    50: invokeinterface java/util/List.add:(Ljava/lang/Object;)Z
        //    55: pop
        //    56: aload           worker
        //    58: invokevirtual   java/lang/Thread.start:()V
        //    61: return
        //
        // The error that occurred was:
        //
        // java.lang.IllegalStateException: Unsupported node type: com.strobel.decompiler.ast.Lambda
        //     at com.strobel.decompiler.ast.Error.unsupportedNode(Error.java:32)
        //     at com.strobel.decompiler.ast.GotoRemoval.exit(GotoRemoval.java:612)
        //     at com.strobel.decompiler.ast.GotoRemoval.exit(GotoRemoval.java:586)
        //     at com.strobel.decompiler.ast.GotoRemoval.transformLeaveStatements(GotoRemoval.java:625)
        //     at com.strobel.decompiler.ast.GotoRemoval.removeGotosCore(GotoRemoval.java:57)
        //     at com.strobel.decompiler.ast.GotoRemoval.removeGotos(GotoRemoval.java:53)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:276)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:42)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:214)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:99)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:782)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:675)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:552)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:519)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:161)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:150)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:125)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at com.strobel.decompiler.DecompilerDriver.decompileType(DecompilerDriver.java:330)
        //     at com.strobel.decompiler.DecompilerDriver.decompileJar(DecompilerDriver.java:251)
        //     at com.strobel.decompiler.DecompilerDriver.main(DecompilerDriver.java:126)
        //
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }

    private void stoplocalPortMap() {
        this.createPortMapBtn.setText("开启");
        final String targetIP = this.portMapTargetIPText.getText();
        final String targetPort = this.portMapTargetPortText.getText();
        final Runnable runner = () -> {
            try {

                Thread thread;
                Iterator<Thread> iterator = this.localList.iterator();
                while (iterator.hasNext()) {
                    thread = iterator.next();
                    thread.interrupt();
                }
                this.currentShellService.closeLocalPortMap(targetIP, targetPort);
                if (this.localPortMapSocket != null && !this.localPortMapSocket.isClosed()) {
                    try {
                        this.localPortMapSocket.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[INFO]本地监听端口已关闭。\n"));
            }
            catch (Exception e2) {
                e2.printStackTrace();
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[ERROR]隧道关闭失败:" + e2.getMessage() + "\n"));
            }
            return;
        };
        final Thread worker = new Thread(runner);
        this.workList.add(worker);
        worker.start();
    }

    private void stopRemotePortMap() {
        this.createPortMapBtn.setText("开启");
        final Runnable runner = () -> {
            try {
                this.currentShellService.closeRemotePortMap();
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[INFO]隧道已关闭，远端相关资源已释放。\n"));
            }
            catch (Exception e) {
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[ERROR]隧道关闭失败:" + e.getMessage() + "\n"));
            }
            return;
        };
        final Thread worker = new Thread(runner);
        this.workList.add(worker);
        worker.start();
    }

    private void createRemotePortMap() {
        this.createPortMapBtn.setText("关闭");
        final String remoteTargetIP = this.portMapTargetIPText.getText();
        final String remoteTargetPort = this.portMapTargetPortText.getText();
        final String remoteIP = this.portMapIPText.getText();
        final String remotePort = this.portMapPortText.getText();
        final Runnable runner = () -> {
            try {
                this.currentShellService.createRemotePortMap(remoteTargetIP, remoteTargetPort, remoteIP, remotePort);
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[INFO]隧道建立成功，请连接VPS。\n"));
            }
            catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> this.tunnelLogTextarea.appendText("[ERROR]隧道建立失败:" + e.getMessage() + "\n"));
            }
            return;
        };
        final Thread worker = new Thread(runner);
        this.workList.add(worker);
        worker.start();
    }

    private void createLocalSocks() {
        this.createSocksBtn.setText("关闭");
        (this.proxyUtils = new ProxyUtils()).start();
    }

    private void stopLocalSocks() {
        this.proxyUtils.shutdown();
        this.createSocksBtn.setText("开启");
    }

    private void createRemoteSocks() {
        this.createSocksBtn.setText("关闭");
        (this.proxyUtils = new ProxyUtils()).start();
    }

    private void stopRemoteSocks() {
        this.proxyUtils.shutdown();
        this.createSocksBtn.setText("开启");
    }

    class ProxyUtils extends Thread
    {
        private Thread r;
        private Thread w;
        private Thread proxy;
        private ServerSocket serverSocket;
        private int bufSize;

        ProxyUtils() {
            this.bufSize = 65535;
        }

        private void log(final String type, final String log) {
            final String logLine = "[" + type + "]" + log + "\n";
            Platform.runLater(() -> TunnelViewController.this.tunnelLogTextarea.appendText(logLine));
        }

        public void shutdown() {
            this.log("INFO", "正在关闭代理服务");
            try {
                if (this.r != null) {
                    this.r.stop();
                }
                if (this.w != null) {
                    this.w.stop();
                }
                if (this.proxy != null) {
                    this.proxy.stop();
                }
                this.serverSocket.close();
            }
            catch (IOException e) {
                this.log("ERROR", "代理服务关闭异常:" + e.getMessage());
            }
            this.log("INFO", "代理服务已停止");
            TunnelViewController.this.createSocksBtn.setText("开启");
        }

        @Override
        public void run() {
            try {
                final String socksPort = TunnelViewController.this.socksPortText.getText();
                final String socksIP = TunnelViewController.this.socksIPText.getText();
                this.proxy = Thread.currentThread();
                (this.serverSocket = new ServerSocket(Integer.parseInt(socksPort), 50, InetAddress.getByName(socksIP))).setReuseAddress(true);
                this.log("INFO", "正在监听端口" + socksPort);
                while (true) {
                    final Socket socket = this.serverSocket.accept();
                    this.log("INFO", "收到客户端连接请求.");
                    new Session(socket).start();
                }
            }
            catch (IOException e) {
                this.log("ERROR", "端口监听失败：" + e.getMessage());
            }
        }

        private class Session extends Thread
        {
            private Socket socket;

            public Session(final Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try {
                    if (this.handleSocks(this.socket)) {
                        ProxyUtils.this.log("INFO", "正在通信...");
                        ProxyUtils.this.r = new Reader();
                        ProxyUtils.this.w = new Writer();
                        ProxyUtils.this.r.start();
                        ProxyUtils.this.w.start();
                        ProxyUtils.this.r.join();
                        ProxyUtils.this.w.join();
                    }
                }
                catch (Exception e2) {
                    try {
                        TunnelViewController.this.currentShellService.closeProxy();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }

            private boolean handleSocks(final Socket socket) throws Exception {
                final int ver = socket.getInputStream().read();
                if (ver == 5) {
                    return this.parseSocks5(socket);
                }
                return ver == 4 && this.parseSocks4(socket);
            }

            private boolean parseSocks5(final Socket socket) throws Exception {
                final DataInputStream ins = new DataInputStream(socket.getInputStream());
                final DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                final int nmethods = ins.read();
                final int methods = ins.read();
                os.write(new byte[] { 5, 0 });
                int version = ins.read();
                int cmd;
                int atyp;
                if (version == 2) {
                    version = ins.read();
                    cmd = ins.read();
                    final int rsv = ins.read();
                    atyp = ins.read();
                }
                else {
                    cmd = ins.read();
                    final int rsv = ins.read();
                    atyp = ins.read();
                }
                final byte[] targetPort = new byte[2];
                String host = "";
                if (atyp == 1) {
                    final byte[] target = new byte[4];
                    ins.readFully(target);
                    ins.readFully(targetPort);
                    final String[] tempArray = new String[4];
                    for (int i = 0; i < target.length; ++i) {
                        final int temp = target[i] & 0xFF;
                        tempArray[i] = temp + "";
                    }
                    for (final String temp2 : tempArray) {
                        host = host + temp2 + ".";
                    }
                    host = host.substring(0, host.length() - 1);
                }
                else if (atyp == 3) {
                    final int targetLen = ins.read();
                    final byte[] target = new byte[targetLen];
                    ins.readFully(target);
                    ins.readFully(targetPort);
                    host = new String(target);
                }
                else if (atyp == 4) {
                    final byte[] target = new byte[16];
                    ins.readFully(target);
                    ins.readFully(targetPort);
                    host = new String(target);
                }
                final int port = (targetPort[0] & 0xFF) * 256 + (targetPort[1] & 0xFF);
                if (cmd == 2 || cmd == 3) {
                    throw new Exception("not implemented");
                }
                if (cmd != 1) {
                    throw new Exception("Socks5 - Unknown CMD");
                }
                host = InetAddress.getByName(host).getHostAddress();
                if (TunnelViewController.this.currentShellService.openProxy(host, port + "")) {
                    os.write(CipherUtils.mergeByteArray(new byte[][] { { 5, 0, 0, 1 }, InetAddress.getByName(host).getAddress(), targetPort }));
                    ProxyUtils.this.log("INFO", "隧道建立成功，请求远程地址" + host + ":" + port);
                    return true;
                }
                os.write(CipherUtils.mergeByteArray(new byte[][] { { 5, 0, 0, 1 }, InetAddress.getByName(host).getAddress(), targetPort }));
                throw new Exception(String.format("[%s:%d] Remote failed", host, port));
            }

            private boolean parseSocks4(final Socket socket) {
                return false;
            }

            private class Reader extends Thread
            {
                @Override
                public void run() {
                    while (Session.this.socket != null) {
                        try {
                            final byte[] data = TunnelViewController.this.currentShellService.readProxyData();
                            if (data == null) {
                                break;
                            }
                            if (data.length == 0) {
                                Thread.sleep(100L);
                            }
                            else {
                                Session.this.socket.getOutputStream().write(data);
                                Session.this.socket.getOutputStream().flush();
                            }
                        }
                        catch (Exception e) {
                            ProxyUtils.this.log("ERROR", "数据读取异常:" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }

            private class Writer extends Thread
            {
                @Override
                public void run() {
                    while (true) {
                        while (Session.this.socket != null) {
                            try {
                                Session.this.socket.setSoTimeout(1000);
                                byte[] data = new byte[ProxyUtils.this.bufSize];
                                final int length = Session.this.socket.getInputStream().read(data);
                                if (length != -1) {
                                    data = Arrays.copyOfRange(data, 0, length);
                                    TunnelViewController.this.currentShellService.writeProxyData(data);
                                    continue;
                                }
                            }
                            catch (SocketTimeoutException e2) {
                                continue;
                            }
                            catch (Exception e) {
                                ProxyUtils.this.log("ERROR", "数据写入异常:" + e.getMessage());
                                e.printStackTrace();
                            }
                            try {
                                TunnelViewController.this.currentShellService.closeProxy();
                                ProxyUtils.this.log("INFO", "隧道关闭成功。");
                                Session.this.socket.close();
                            }
                            catch (Exception e) {
                                ProxyUtils.this.log("ERROR", "隧道关闭失败:" + e.getMessage());
                                e.printStackTrace();
                            }
                            return;
                        }
                        continue;
                    }
                }
            }
        }
    }
}
