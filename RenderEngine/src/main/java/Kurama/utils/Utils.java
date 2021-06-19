package Kurama.utils;

import Kurama.Math.Perlin;
import Kurama.Math.Vector;
import org.lwjgl.BufferUtils;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.BufferUtils.createByteBuffer;

public class Utils {
	
	public static Vector projectPointToPlane(Vector e1, Vector e2, Vector p) {
		Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
		Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
		return proj1.add(proj2);
	}

	public static String getUniqueID() {
		return UUID.randomUUID().toString();
	}

	public static String loadResourceAsString(String filename) throws IOException {
		StringBuilder temp = new StringBuilder();

		Logger.log(filename);

		File file = new File(filename);
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while((line = br.readLine()) != null) {
				temp.append(line);
				temp.append("\n");
			}
		}catch (Exception e) {
			Logger.logError("Couldn't load file: "+filename);
			throw new IOException("Could not load file");
		}

		return temp.toString();
	}

	public static Path getClassPath(String searchClass) {
		Path result=null;

		try (Stream<Path> stream = Files.find(Paths.get(System.getProperty("user.dir")), 5,
				(path, attr) -> path.getFileName().toString().equals(searchClass) )) {
			result = stream.findAny().get();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static long generateSeed(String in) {
		long seed = 0;

		StringBuilder binary = new StringBuilder();
		for (byte b : in.getBytes())
		{
			int val = b;
			for (int i = 0; i < 8; i++)
			{
				binary.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}

		seed = 0;
		int counter = 1;
		for(int c: in.toCharArray()) {
			seed += c * (13 *Perlin.octavePerlin(c*counter*0.1f,c*counter*0.1f,c*counter*0.1f,5,0.4f));
			counter++;
		}

		return seed;
	}

	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;

		Path path = Paths.get(resource);
		if(!Files.exists(path)) {
			Logger.logError("resource doesn't exist");
		}


		if (Files.isReadable(path)) {
			try (SeekableByteChannel fc = Files.newByteChannel(path)) {
				buffer = createByteBuffer((int) fc.size() + 1);
				while (fc.read(buffer) != -1) ;
			}
		} else {
			URL url = Utils.class.getResource(resource);
			try (
					InputStream source = url.openStream();
					ReadableByteChannel rbc = Channels.newChannel(source)) {
				buffer = createByteBuffer(bufferSize);

				while (true) {
					int bytes = rbc.read(buffer);
					if (bytes == -1) {
						break;
					}
					if (buffer.remaining() == 0) {
						buffer = resizeBuffer(buffer, buffer.capacity() * 2);
					}
				}
			}
		}

		buffer.flip();
		return buffer;
	}

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}


//	Code adapted from https://www.baeldung.com/java-reflection-class-fields

	public static List<Field> getAllDeclaredFields(Class clazz) {
		if(clazz == null) return Collections.emptyList();

		List<Field> result = new ArrayList<>(getAllDeclaredFields(clazz.getSuperclass()));

		result.addAll(Arrays.asList(clazz.getDeclaredFields()));
		return result;
	}

	public static List<Field> getAllDeclaredFieldsByAnnotation(Class clazz, Class<? extends Annotation> annotation) {
		if(clazz == null) return Collections.emptyList();

		List<Field> result = new ArrayList<>(getAllDeclaredFieldsByAnnotation(clazz.getSuperclass(), annotation));

		var filtered = Arrays.stream(clazz.getDeclaredFields()).filter(f -> f.isAnnotationPresent(annotation)).collect(Collectors.toList());
		result.addAll(filtered);
		return result;
	}


}
