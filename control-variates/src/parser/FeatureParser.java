package parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Two arguments:
 * 1) filename for new features output file (aggregated obs of player features)
 *    by role-symmetry group
 * 2) filename to stream (full game json) - could be multiple files
 *    
 * @author ewah
 *
 */
public class FeatureParser {

	public static void main(String... args) {

		String[] filenames = null;
		File featureJsonFile = new File(".");
		if (args.length >= 1) {
			featureJsonFile = new File(args[0]);
		}
		if (args.length >= 2) {
			filenames = new String[args.length-1];
			for (int i = 0; i < filenames.length; i++) {
				filenames[i] = args[i+1];
			}
		} else {
			System.exit(1);
		}
		try {
			JsonStreamer streamer = new JsonStreamer();
			if (filenames != null) {
				for (String filename : filenames) {
					streamer.readJsonStream(new FileInputStream(filename));
				}
				streamer.writeFeaturesToJson(featureJsonFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
