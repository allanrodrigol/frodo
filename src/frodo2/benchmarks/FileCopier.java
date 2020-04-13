/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<https://frodo-ai.tech>
 */

package frodo2.benchmarks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Helper utility that copies a file for use by frodo2.py when running experiments from a repository of problem instances
 * @author Thomas Leaute
 */
public class FileCopier {

	/** Copies a file
	 * @param args [originFile, targetFile]
	 * @throws IOException if the copy fails
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length != 2) {
			System.err.println("The FileCopier requires two input arguments: the path to the source file, and the path to the target file");
			System.exit(1);
		}
		
		Files.copy(Paths.get(args[0]), Paths.get(args[1]), StandardCopyOption.REPLACE_EXISTING);
		
		System.out.println("Copied " + args[0] + " to " + args[1]);
	}

}
