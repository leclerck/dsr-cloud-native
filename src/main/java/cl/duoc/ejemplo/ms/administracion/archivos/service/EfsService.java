package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	public File saveToEfs(String filename, MultipartFile multipartFile) throws IOException {

		File dest = new File(efsPath, filename);
		File parentDir = dest.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		multipartFile.transferTo(dest);
		return dest;
	}

	public File saveBytes(String key, byte[] content) throws IOException {

		File dest = new File(efsPath, key);
		File parentDir = dest.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		Files.write(dest.toPath(), content);
		return dest;
	}

	public void delete(String key) throws IOException {

		Path path = Path.of(efsPath, key);
		Files.deleteIfExists(path);
	}
}
