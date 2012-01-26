// Copyright 2011 Denis Stepanov
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package coffeescript.nb.project;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager.Result;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Denis Stepanov
 */
@org.openide.util.lookup.ServiceProvider(service = ProjectFactory.class)
public class CoffeeScriptProjectFactory implements ProjectFactory2 {

    public final static String PROJECT_CONFIG_FILE = "Cakefile";

    public Result isProject2(FileObject projectDirectory) {
        return isProject(projectDirectory) ? new Result(null) : null;
    }

    public boolean isProject(FileObject projectDirectory) {
        return projectDirectory.getFileObject(PROJECT_CONFIG_FILE) != null;
    }

    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        return isProject(projectDirectory) ? new CoffeeScriptProject(projectDirectory, state) : null;
    }

    public void saveProject(Project project) throws IOException, ClassCastException {
//        FileObject projectRoot = project.getProjectDirectory();
//        if (projectRoot.getFileObject(PROJECT_CONFIG_FILE) == null) {
//            throw new IOException("Project file " + projectRoot.getPath()
//                    + " deleted,"
//                    + " cannot save project");
//        }
        ((CoffeeScriptProject) project).getConfigFile(true);
    }
}
