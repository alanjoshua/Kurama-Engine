package engine.utils;

import engine.Math.Perlin;
import engine.Math.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

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

}
