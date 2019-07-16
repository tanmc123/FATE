package com.webank.ai.fate.board.ssh;

import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.webank.ai.fate.board.pojo.SshInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class SftpUtils {
    private static Logger log = LoggerFactory.getLogger(SftpUtils.class.getName());

    public static ChannelSftp connect(SshInfo sshInfo) {
        try {
            JSch jsch = new JSch();
            Session sshSession = jsch.getSession(sshInfo.getUser(), sshInfo.getIp(), sshInfo.getPort());

            if (log.isInfoEnabled()) {
                log.info("Session created.");
            }
            sshSession.setPassword(sshInfo.getPassword());
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            if (log.isInfoEnabled()) {
                log.info("Session connected.");
            }
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            if (log.isInfoEnabled()) {
                log.info("Opening Channel.");
            }

            return (ChannelSftp) channel;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static void disconnect(ChannelSftp channelSftp) {
        if (channelSftp != null) {
            if (channelSftp.isConnected()) {
                channelSftp.disconnect();
                if (log.isInfoEnabled()) {
                    log.info("sftp is closed already");
                }
            }

            try {
                if (channelSftp.getSession() != null) {
                    if (channelSftp.getSession().isConnected()) {
                        channelSftp.getSession().disconnect();
                    }
                }
            } catch (JSchException e) {
                e.printStackTrace();
            }
        }

    }


    public static List<String> batchDownLoadFile(SshInfo sshInfo,
                                                 String remotePath,
                                                 String localPath,
                                                 String fileFormat,
                                                 String fileEndFormat,
                                                 boolean del) {


        ChannelSftp channelSftp = null;
        channelSftp = connect(sshInfo);
        try {
            return batchDownLoadFileInner(channelSftp, remotePath, localPath, fileFormat, fileEndFormat, del);
        } finally {
            if (channelSftp != null) {
                try {
                    if (channelSftp.getSession() != null) {
                        channelSftp.getSession().disconnect();
                    }
                } catch (JSchException e) {
                    e.printStackTrace();
                }
                channelSftp.disconnect();
            }
        }
    }


    private static List<String> batchDownLoadFileInner(
            ChannelSftp channelSftp,
            String remotePath,
            String localPath,
            String fileFormat,
            String fileEndFormat,
            boolean del) {
        mkdirs(localPath);


        List<String> filenames = new ArrayList<String>();

        try {

            Vector v = listFiles(channelSftp, remotePath);
            // sftp.cd(remotePath);
            if (v.size() > 0) {
                System.out.println("本次处理文件个数不为零,开始下载...fileSize=" + v.size());
                Iterator it = v.iterator();
                while (it.hasNext()) {
                    LsEntry entry = (LsEntry) it.next();
                    String filename = entry.getFilename();
                    SftpATTRS attrs = entry.getAttrs();
                    if (!attrs.isDir()) {
                        boolean flag = false;
                        String localFileName = localPath + filename;
                        fileFormat = fileFormat == null ? "" : fileFormat
                                .trim();
                        fileEndFormat = fileEndFormat == null ? ""
                                : fileEndFormat.trim();
                        // 三种情况
                        if (fileFormat.length() > 0 && fileEndFormat.length() > 0) {
                            if (filename.startsWith(fileFormat) && filename.endsWith(fileEndFormat)) {
                                flag = downloadFile(channelSftp, remotePath, filename, localPath, filename);
                                if (flag) {
                                    filenames.add(localFileName);
                                    if (flag && del) {
                                        deleteSFTP(channelSftp, remotePath, filename);
                                    }
                                }
                            }
                        } else if (fileFormat.length() > 0 && "".equals(fileEndFormat)) {
                            if (filename.startsWith(fileFormat)) {
                                flag = downloadFile(channelSftp, remotePath, filename, localPath, filename);
                                if (flag) {
                                    filenames.add(localFileName);
                                    if (flag && del) {
                                        deleteSFTP(channelSftp, remotePath, filename);
                                    }
                                }
                            }
                        } else if (fileEndFormat.length() > 0 && "".equals(fileFormat)) {
                            if (filename.endsWith(fileEndFormat)) {
                                flag = downloadFile(channelSftp, remotePath, filename, localPath, filename);
                                if (flag) {
                                    filenames.add(localFileName);
                                    if (flag && del) {
                                        deleteSFTP(channelSftp, remotePath, filename);
                                    }
                                }
                            }
                        } else {
                            flag = downloadFile(channelSftp, remotePath, filename, localPath, filename);
                            if (flag) {
                                filenames.add(localFileName);
                                if (flag && del) {
                                    deleteSFTP(channelSftp, remotePath, filename);
                                }
                            }
                        }
                    } else {
                        if (!filename.equals(".") && !filename.equals("..")) {
                            batchDownLoadFileInner(channelSftp,
                                    remotePath + filename + "/",
                                    localPath + filename + "/",
                                    fileFormat,
                                    fileEndFormat,
                                    del);

                        }
                    }
                }
            }
            if (log.isInfoEnabled()) {
                log.info("download file is success:remotePath=" + remotePath
                        + "and localPath=" + localPath + ",file size is"
                        + v.size());
            }
        } catch (SftpException e) {
            e.printStackTrace();
        }

        return filenames;
    }


    public static boolean downloadFile(ChannelSftp sftp, String remotePath, String remoteFileName, String localPath, String localFileName) {
        FileOutputStream fieloutput = null;
        try {
            // sftp.cd(remotePath);

            if (log.isInfoEnabled()) {
                log.info("remote file {} : localfile {}", remotePath + remoteFileName, localPath + localFileName);
            }

            File file = new File(localPath + localFileName);
            // mkdirs(localPath + localFileName);
            fieloutput = new FileOutputStream(file);


            sftp.get(remotePath + remoteFileName, fieloutput);

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } finally {
            if (null != fieloutput) {
                try {
                    fieloutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }



    /**
     * 删除本地文件
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        if (!file.isFile()) {
            return false;
        }
        boolean rs = file.delete();
        if (rs && log.isInfoEnabled()) {
            log.info("delete file success from local.");
        }
        return rs;
    }


    /**
     * 判断目录是否存在
     *
     * @param directory
     * @return
     */
    public static boolean isDirExist(ChannelSftp channelSftp, String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = channelSftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }

    /**
     * 删除stfp文件
     *
     * @param directory：要删除文件所在目录
     * @param deleteFile：要删除的文件
     */
    public static void deleteSFTP(ChannelSftp channelSftp, String directory, String deleteFile) {
        try {
            // sftp.cd(directory);
            channelSftp.rm(directory + deleteFile);
            if (log.isInfoEnabled()) {
                log.info("delete file success from sftp.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果目录不存在就创建目录
     *
     * @param path
     */
    public static void mkdirs(String path) {
        File f = new File(path);

        String fs = f.getParent();

        f = new File(fs);

        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * 列出目录下的文件
     *
     * @param directory：要列出的目录
     * @return
     * @throws SftpException
     */
    public static Vector listFiles(ChannelSftp sftp, String directory) throws SftpException {
        return sftp.ls(directory);
    }


//    /**测试*/
//    public static void main(String[] args)
//    {
//        SFTPUtils sftp = null;
//        // 本地存放地址
//        String localPath = "D:/tomcat5/webapps/ASSESS/DocumentsDir/DocumentTempDir/txtData/";
//        // Sftp下载路径
//        String sftpPath = "/home/assess/sftp/jiesuan_2/2014/";
//        List<String> filePathList = new ArrayList<String>();
//        try
//        {
//            sftp = new SFTPUtils("10.163.201.115", "tdcp", "tdcp");
//            sftp.connect();
//            // 下载
//            sftp.batchDownLoadFile(sftpPath, localPath, "ASSESS", ".txt", true);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        finally
//        {
//            sftp.disconnect();
//        }
//    }


///**
// * 时间处理工具类（简单的）
// * @author Aaron
// * @date 2014-6-17
// * @time 下午1:39:44
// * @version 1.0
// */
//public class DateUtil {
//     /**
//     * 默认时间字符串的格式
//     */
//    public static final String DEFAULT_FORMAT_STR = "yyyyMMddHHmmss";
//
//    public static final String DATE_FORMAT_STR = "yyyyMMdd";
//
//    /**
//     * 获取系统时间的昨天
//     * @return
//     */
//    public static String getSysTime(){
//         Calendar ca = Calendar.getInstance();
//         ca.set(Calendar.DATE, ca.get(Calendar.DATE)-1);
//         Date d = ca.getTime();
//         SimpleDateFormat sdf =  new SimpleDateFormat("yyyyMMdd");
//         String a = sdf.format(d);
//        return a;
//    }
//
//    /**
//     * 获取当前时间
//     * @param date
//     * @return
//     */
//    public static String getCurrentDate(String formatStr)
//    {
//        if (null == formatStr)
//        {
//            formatStr=DEFAULT_FORMAT_STR;
//        }
//        return date2String(new Date(), formatStr);
//    }
//
//    /**
//     * 返回年月日
//     * @return yyyyMMdd
//     */
//    public static String getTodayChar8(String dateFormat){
//        return DateFormatUtils.format(new Date(), dateFormat);
//    }
//
//    /**
//     * 将Date日期转换为String
//     * @param date
//     * @param formatStr
//     * @return
//     */
//    public static String date2String(Date date, String formatStr)
//    {
//        if (null == date || null == formatStr)
//        {
//            return "";
//        }
//        SimpleDateFormat df = new SimpleDateFormat(formatStr);
//
//        return df.format(date);
//    }
}