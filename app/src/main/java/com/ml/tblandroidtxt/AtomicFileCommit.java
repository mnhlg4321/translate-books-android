package com.ml.tblandroidtxt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
public final class AtomicFileCommit {
    public static Result write(Path destination,String text) throws IOException {
        if(destination==null)throw new IllegalArgumentException("destination missing");if(text==null||text.isEmpty())throw new IllegalArgumentException("refusing empty output");
        Path parent=destination.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);Path temp=Files.createTempFile(parent,destination.getFileName().toString(),".pending"),backup=null;
        try{Files.write(temp,text.getBytes(StandardCharsets.UTF_8));if(!HashUtil.sha256(text).equals(HashUtil.sha256(new String(Files.readAllBytes(temp),StandardCharsets.UTF_8))))throw new IOException("stage verification failed");if(Files.exists(destination)){backup=destination.resolveSibling(destination.getFileName()+".recovery");Files.copy(destination,backup,StandardCopyOption.REPLACE_EXISTING);}try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}String reopened=new String(Files.readAllBytes(destination),StandardCharsets.UTF_8);if(!HashUtil.sha256(text).equals(HashUtil.sha256(reopened)))throw new IOException("commit verification failed");return new Result(destination,backup,HashUtil.sha256(reopened));}finally{Files.deleteIfExists(temp);}
    }
    public static class Result{public final Path destination,backup;public final String sha256;Result(Path d,Path b,String h){destination=d;backup=b;sha256=h;}}private AtomicFileCommit(){}
}
