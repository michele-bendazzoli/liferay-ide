package com.liferay.ide.eclipse.theme.core.util;

import com.liferay.ide.eclipse.theme.core.ThemeCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.internal.Messages;
import org.eclipse.wst.server.core.internal.ProgressUtil;

@SuppressWarnings( "restriction" )
public class BuildHelper
{

    // size of the buffer
    private static final int BUFFER = 65536;

    // the buffer
    private static byte[] buf = new byte[BUFFER];

    private static final IStatus[] EMPTY_STATUS = new IStatus[0];

    private static final File defaultTempDir = ThemeCore.getDefault().getStateLocation().toFile();

    private static final String TEMPFILE_PREFIX = "tmp";

    private File tempDir;

    /**
     * Create a new PublishHelper.
     *
     * @param tempDirectory
     *            a temporary directory to use during publishing, or <code>null</code> to use the default. If it does
     *            not exist, the folder will be created
     */
    public BuildHelper()
    {
        tempDir = defaultTempDir;
        if( !tempDir.exists() )
        {
            tempDir.mkdirs();
        }
    }

    /**
     * Copy a file from a to b. Closes the input stream after use.
     *
     * @param in
     *            an input stream
     * @param to
     *            a path to copy to. the directory must already exist
     * @param ts
     *            timestamp
     * @throws CoreException
     *             if anything goes wrong
     */
    private void copyFile( InputStream in, IPath to, long ts, IFile mf ) throws CoreException
    {
        OutputStream out = null;

        File tempFile = null;
        try
        {
            File file = to.toFile();
            tempFile = File.createTempFile( TEMPFILE_PREFIX, "." + to.getFileExtension(), tempDir );

            out = new FileOutputStream( tempFile );

            int avail = in.read( buf );
            while( avail > 0 )
            {
                out.write( buf, 0, avail );
                avail = in.read( buf );
            }

            out.close();
            out = null;

            moveTempFile( tempFile, file );

            if( ts != IResource.NULL_STAMP && ts != 0 )
                file.setLastModified( ts );
        }
        catch( CoreException e )
        {
            throw e;
        }
        catch( Exception e )
        {
        }
        finally
        {
            if( tempFile != null && tempFile.exists() )
                tempFile.deleteOnExit();
            try
            {
                if( in != null )
                    in.close();
            }
            catch( Exception ex )
            {
                // ignore
            }
            try
            {
                if( out != null )
                    out.close();
            }
            catch( Exception ex )
            {
                // ignore
            }
        }
    }

    /**
     * Utility method to recursively delete a directory.
     *
     * @param dir
     *            a directory
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public static IStatus[] deleteDirectory( File dir, IProgressMonitor monitor )
    {
        if( !dir.exists() || !dir.isDirectory() )
            return new IStatus[] { new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                Messages.errorNotADirectory, dir.getAbsolutePath() ), null ) };

        List<IStatus> status = new ArrayList<IStatus>( 2 );

        try
        {
            File[] files = dir.listFiles();
            int size = files.length;
            monitor = ProgressUtil.getMonitorFor( monitor );
            monitor.beginTask( NLS.bind( Messages.deletingTask, new String[] { dir.getAbsolutePath() } ), size * 10 );

            // cycle through files
            boolean deleteCurrent = true;
            for( int i = 0; i < size; i++ )
            {
                File current = files[i];
                if( current.isFile() )
                {
                    if( !current.delete() )
                    {
                        status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                            Messages.errorDeleting, files[i].getAbsolutePath() ), null ) );
                        deleteCurrent = false;
                    }
                    monitor.worked( 10 );
                }
                else if( current.isDirectory() )
                {
                    monitor.subTask( NLS.bind( Messages.deletingTask, new String[] { current.getAbsolutePath() } ) );
                    IStatus[] stat = deleteDirectory( current, ProgressUtil.getSubMonitorFor( monitor, 10 ) );
                    if( stat != null && stat.length > 0 )
                    {
                        deleteCurrent = false;
                        addArrayToList( status, stat );
                    }
                }
            }
            if( deleteCurrent && !dir.delete() )
                status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                    Messages.errorDeleting, dir.getAbsolutePath() ), null ) );
            monitor.done();
        }
        catch( Exception e )
        {
            ThemeCore.logError( "Error deleting directory " + dir.getAbsolutePath(), e );
            status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, e.getLocalizedMessage(), null ) );
        }

        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    /**
     * Smart copy the given module resources to the given path.
     *
     * @param resources
     *            an array of module resources
     * @param path
     *            an external path to copy to
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishSmart( IResource[] resources, IPath path, IProgressMonitor monitor )
    {
        return publishSmart( resources, path, null, monitor );
    }

    /**
     * Smart copy the given module resources to the given path.
     *
     * @param resources
     *            an array of module resources
     * @param path
     *            an external path to copy to
     * @param ignore
     *            an array of paths relative to path to ignore, i.e. not delete or copy over
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishSmart( IResource[] resources, IPath path, IPath[] ignore, IProgressMonitor monitor )
    {
        if( resources == null )
            return EMPTY_STATUS;

        monitor = ProgressUtil.getMonitorFor( monitor );

        List<IStatus> status = new ArrayList<IStatus>( 2 );
        File toDir = path.toFile();
        int fromSize = resources.length;
        String[] fromFileNames = new String[fromSize];
        for( int i = 0; i < fromSize; i++ )
            fromFileNames[i] = resources[i].getName();
        List<String> ignoreFileNames = new ArrayList<String>();
        if( ignore != null )
        {
            for( int i = 0; i < ignore.length; i++ )
            {
                if( ignore[i].segmentCount() == 1 )
                {
                    ignoreFileNames.add( ignore[i].toOSString() );
                }
            }
        }

        // cache files and file names for performance
        File[] toFiles = null;
        String[] toFileNames = null;

        boolean foundExistingDir = false;
        if( toDir.exists() )
        {
            if( toDir.isDirectory() )
            {
                foundExistingDir = true;
                toFiles = toDir.listFiles();
                int toSize = toFiles.length;
                toFileNames = new String[toSize];

                // check if this exact file exists in the new directory
                for( int i = 0; i < toSize; i++ )
                {
                    toFileNames[i] = toFiles[i].getName();
                    boolean isDir = toFiles[i].isDirectory();
                    boolean found = false;
                    for( int j = 0; j < fromSize; j++ )
                    {
                        if( toFileNames[i].equals( fromFileNames[j] ) && isDir == resources[j] instanceof IFolder )
                        {
                            found = true;
                            break;
                        }
                    }

                    // delete file if it can't be found or isn't the correct type
                    if( !found )
                    {
                        boolean delete = true;
                        // if should be preserved, don't delete and don't try to copy
                        for( String preserveFileName : ignoreFileNames )
                        {
                            if( toFileNames[i].equals( preserveFileName ) )
                            {
                                delete = false;
                                break;
                            }
                        }
                        if( delete )
                        {
                            if( isDir )
                            {
                                IStatus[] stat = deleteDirectory( toFiles[i], null );
                                addArrayToList( status, stat );
                            }
                            else
                            {
                                if( !toFiles[i].delete() )
                                    status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                                        Messages.errorDeleting, toFiles[i].getAbsolutePath() ), null ) );
                            }
                        }
                        toFiles[i] = null;
                        toFileNames[i] = null;
                    }
                }
            }
            else
            { // if (toDir.isFile())
                if( !toDir.delete() )
                {
                    status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                        Messages.errorDeleting, toDir.getAbsolutePath() ), null ) );
                    IStatus[] stat = new IStatus[status.size()];
                    status.toArray( stat );
                    return stat;
                }
            }
        }
        if( !foundExistingDir && !toDir.mkdirs() )
        {
            status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                Messages.errorMkdir, toDir.getAbsolutePath() ), null ) );
            IStatus[] stat = new IStatus[status.size()];
            status.toArray( stat );
            return stat;
        }

        if( monitor.isCanceled() )
            return new IStatus[] { Status.CANCEL_STATUS };

        monitor.worked( 50 );

        // cycle through files and only copy when it doesn't exist
        // or is newer
        if( toFiles == null )
        {
            toFiles = toDir.listFiles();
            if( toFiles == null )
                toFiles = new File[0];
        }
        int toSize = toFiles.length;

        int dw = 0;
        if( toSize > 0 )
            dw = 500 / toSize;

        // cache file names and last modified dates for performance
        if( toFileNames == null )
            toFileNames = new String[toSize];
        long[] toFileMod = new long[toSize];
        for( int i = 0; i < toSize; i++ )
        {
            if( toFiles[i] != null )
            {
                if( toFileNames[i] != null )
                    toFileNames[i] = toFiles[i].getName();
                toFileMod[i] = toFiles[i].lastModified();
            }
        }

        for( int i = 0; i < fromSize; i++ )
        {
            IResource current = resources[i];
            String name = fromFileNames[i];
            boolean currentIsDir = current instanceof IFolder;

            if( !currentIsDir )
            {
                // check if this is a new or newer file
                boolean copy = true;
                IFile mf = (IFile) current;

                long mod = -1;
                IFile file = (IFile) mf.getAdapter( IFile.class );
                if( file != null )
                {
                    mod = file.getLocalTimeStamp();
                }
                else
                {
                    File file2 = (File) mf.getAdapter( File.class );
                    mod = file2.lastModified();
                }

                for( int j = 0; j < toSize; j++ )
                {
                    if( name.equals( toFileNames[j] ) && mod == toFileMod[j] )
                    {
                        copy = false;
                        break;
                    }
                }

                if( copy )
                {
                    try
                    {
                        copyFile( mf, path.append( name ) );
                    }
                    catch( CoreException ce )
                    {
                        status.add( ce.getStatus() );
                    }
                }
                monitor.worked( dw );
            }
            else
            { // if (currentIsDir) {
                IFolder folder = (IFolder) current;
                IResource[] children = null;

                try
                {
                    children = folder.members();
                }
                catch( CoreException e )
                {
                    e.printStackTrace();
                }

                // build array of ignored Paths that apply to this folder
                IPath[] ignoreChildren = null;
                if( ignore != null )
                {
                    List<IPath> ignoreChildPaths = new ArrayList<IPath>();
                    for( int j = 0; j < ignore.length; j++ )
                    {
                        IPath preservePath = ignore[j];
                        if( preservePath.segment( 0 ).equals( name ) )
                        {
                            ignoreChildPaths.add( preservePath.removeFirstSegments( 1 ) );
                        }
                    }
                    if( ignoreChildPaths.size() > 0 )
                        ignoreChildren = ignoreChildPaths.toArray( new Path[ignoreChildPaths.size()] );
                }
                monitor.subTask( NLS.bind( Messages.copyingTask, new String[] { name, name } ) );
                IStatus[] stat =
                    publishSmart(
                        children, path.append( name ), ignoreChildren, ProgressUtil.getSubMonitorFor( monitor, dw ) );
                addArrayToList( status, stat );
            }
        }
        if( monitor.isCanceled() )
            return new IStatus[] { Status.CANCEL_STATUS };

        monitor.worked( 500 - dw * toSize );
        monitor.done();

        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    /**
     * Handle a delta publish.
     *
     * @param delta
     *            a module resource delta
     * @param path
     *            the path to publish to
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishDelta( IResourceDelta[] delta, IPath path, IPath[] restorePaths, IProgressMonitor monitor )
    {
        if( delta == null )
            return EMPTY_STATUS;

        monitor = ProgressUtil.getMonitorFor( monitor );

        List<IStatus> status = new ArrayList<IStatus>( 2 );
        int size2 = delta.length;
        for( int i = 0; i < size2; i++ )
        {
            IStatus[] stat = publishDelta( delta[i], path, restorePaths, monitor );
            addArrayToList( status, stat );
        }

        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    /**
     * Handle a delta publish.
     *
     * @param delta
     *            a module resource delta
     * @param path
     *            the path to publish to
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishDelta( IResourceDelta delta, IPath path, IPath[] restorePaths, IProgressMonitor monitor )
    {
        List<IStatus> status = new ArrayList<IStatus>( 2 );

        IResource resource = delta.getResource();
        int kind2 = delta.getKind();

        if( resource instanceof IFile )
        {
            IFile file = (IFile) resource;

            try
            {
                if( kind2 == IResourceDelta.REMOVED )
                {
                    deleteFile( path, file, restorePaths );
                }
                else
                {
                    IPath diffsRelativePath = getDiffsRelativePath(file.getProjectRelativePath());

                    if (diffsRelativePath != null)
                    {
                        IPath path2 = path.append( diffsRelativePath );
                        File f = path2.toFile().getParentFile();

                        if( !f.exists() )
                        {
                            f.mkdirs();
                        }

                        copyFile( file, path2 );
                    }
                }
            }
            catch( CoreException ce )
            {
                status.add( ce.getStatus() );
            }

            IStatus[] stat = new IStatus[status.size()];
            status.toArray( stat );
            return stat;
        }

        if( kind2 == IResourceDelta.ADDED )
        {
            // find relative path from _diffs and append that to path.
            IPath diffsPath = resource.getProjectRelativePath();
            IPath diffsRelativePath = getDiffsRelativePath( diffsPath );

            if (diffsRelativePath != null)
            {
                IPath path2 = path.append(diffsRelativePath);
//                IPath path2 = path.append( resource.getProjectRelativePath() ).append( resource.getName() );
                File file = path2.toFile();
                if( !file.exists() && !file.mkdirs() )
                {
                    status.add( new Status(
                        IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind( Messages.errorMkdir, path2 ), null ) );
                    IStatus[] stat = new IStatus[status.size()];
                    status.toArray( stat );
                    return stat;
                }
            }
        }

        IResourceDelta[] childDeltas = delta.getAffectedChildren();
        int size = childDeltas.length;
        for( int i = 0; i < size; i++ )
        {
            IStatus[] stat = publishDelta( childDeltas[i], path, restorePaths, monitor );
            addArrayToList( status, stat );
        }

        if( kind2 == IResourceDelta.REMOVED )
        {
            IPath diffsRelativePath = getDiffsRelativePath( resource.getProjectRelativePath() );

            if (diffsRelativePath != null)
            {
                IPath path2 = path.append(diffsRelativePath);
                //IPath path2 = path.append( resource.getProjectRelativePath() ).append( resource.getName() );
                File file = path2.toFile();

                if( file.exists() && !file.delete() )
                {
                    status.add( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                        Messages.errorDeleting, path2 ), null ) );
                }
            }
        }

        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    private static IPath getDiffsRelativePath(IPath diffsPath)
    {
        IPath diffsRelativePath = null;

        for (int i = 0; i < diffsPath.segmentCount(); i++)
        {
            if ("_diffs".equals(diffsPath.segment( i )))
            {
                diffsRelativePath = diffsPath.removeFirstSegments( i + 1 );
                break;
            }
        }

        return diffsRelativePath;
    }

    private static void deleteFile( IPath path, IFile file, IPath[] restorePaths ) throws CoreException
    {
        IPath diffsPath = file.getProjectRelativePath();
        IPath diffsRelativePath = getDiffsRelativePath( diffsPath );

        if (diffsRelativePath != null)
        {
//            IPath path2 = path.append( file.getProjectRelativePath() ).append( file.getName() );
            IPath path2 = path.append(diffsRelativePath);
            
            // restore this file from the first restorePaths that matches
            boolean restored = false;
            
            for (IPath restorePath : restorePaths)
            {
                final File restoreFile = restorePath.append( diffsRelativePath ).toFile();
                
                if (restoreFile.exists()) 
                {
                    try
                    {
                        FileUtils.copyFile( restoreFile, path2.toFile() );
                        restored = true;
                        break;
                    }
                    catch( IOException e )
                    {
                        throw new CoreException( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                            "Error restoring theme file.", path2 ), null ) );
                    }
                }
            }
            
            if (!restored)
            {
                if( path2.toFile().exists() && !path2.toFile().delete() )
                {
                    throw new CoreException( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                        Messages.errorDeleting, path2 ), null ) );
                }
            }
        }
    }

    private void copyFile( IFile mf, IPath path ) throws CoreException
    {
        if( !isCopyFile( mf, path ) )
        {
            return;
        }

        IFile file = (IFile) mf.getAdapter( IFile.class );
        if( file != null )
            copyFile( file.getContents(), path, file.getLocalTimeStamp(), mf );
        else
        {
            File file2 = (File) mf.getAdapter( File.class );
            InputStream in = null;
            try
            {
                in = new FileInputStream( file2 );
            }
            catch( IOException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                    Messages.errorReading, file2.getAbsolutePath() ), e ) );
            }
            copyFile( in, path, file2.lastModified(), mf );
        }
    }

    /**
     * Returns <code>true<code/> if the module file should be copied to the destination, <code>false</codre> otherwise.
     *
     * @param moduleFile
     *            the module file
     * @param toPath
     *            destination.
     * @return <code>true<code/>, if the module file should be copied
     */
    protected boolean isCopyFile( IFile moduleFile, IPath toPath )
    {
        return true;
    }

    /**
     * Publish the given module resources to the given path.
     *
     * @param resources
     *            an array of module resources
     * @param path
     *            a path to publish to
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishFull( IResource[] resources, IPath path, IProgressMonitor monitor )
    {
        if( resources == null )
            return EMPTY_STATUS;

        monitor = ProgressUtil.getMonitorFor( monitor );

        List<IStatus> status = new ArrayList<IStatus>( 2 );
        int size = resources.length;
        for( int i = 0; i < size; i++ )
        {
            IStatus[] stat = copy( resources[i], path, monitor );
            addArrayToList( status, stat );
        }

        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    private IStatus[] copy( IResource resource, IPath path, IProgressMonitor monitor )
    {
//        String name = resource.getName();
        List<IStatus> status = new ArrayList<IStatus>( 2 );
        if( resource instanceof IFolder )
        {
            IFolder folder = (IFolder) resource;
            IStatus[] stat;
            try
            {
                stat = publishFull( folder.members(), path, monitor );
                addArrayToList( status, stat );
            }
            catch( CoreException e )
            {
                e.printStackTrace();
            }

        }
        else
        {
            IFile mf = (IFile) resource;

            IPath diffsRelativePath = getDiffsRelativePath( mf.getProjectRelativePath());

            if (diffsRelativePath != null)
            {
//              path = path.append( mf.getProjectRelativePath() ).append( name );
                path = path.append(diffsRelativePath);

                File f = path.toFile().getParentFile();
                if( !f.exists() )
                    f.mkdirs();
                try
                {
                    copyFile( mf, path );
                }
                catch( CoreException ce )
                {
                    status.add( ce.getStatus() );
                }
            }



        }
        IStatus[] stat = new IStatus[status.size()];
        status.toArray( stat );
        return stat;
    }

    /**
     * Accepts an IModuleResource array which is expected to contain a single IModuleFile resource and copies it to the
     * specified path, which should include the name of the file to write. If the array contains more than a single
     * resource or the resource is not an IModuleFile resource, the file is not created. Currently no error is returned,
     * but error handling is recommended since that is expected to change in the future.
     *
     * @param resources
     *            an array containing a single IModuleFile resource
     * @param path
     *            the path, including file name, where the file should be created
     * @param monitor
     *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
     * @return a possibly-empty array of error and warning status
     */
    public IStatus[] publishToPath( IResource[] resources, IPath path, IProgressMonitor monitor )
    {
        if( resources == null || resources.length == 0 )
        {
            // should also check if resources consists of all empty directories
            File file = path.toFile();
            if( file.exists() )
                file.delete();
            return EMPTY_STATUS;
        }

        monitor = ProgressUtil.getMonitorFor( monitor );

        if( resources.length == 1 && resources[0] instanceof IFile )
        {
            try
            {
                copyFile( (IFile) resources[0], path );
            }
            catch( CoreException e )
            {
                return new IStatus[] { e.getStatus() };
            }
        }

        return EMPTY_STATUS;
    }

    /**
     * Utility method to move a temp file into position by deleting the original and swapping in a new copy.
     *
     * @param tempFile
     * @param file
     * @throws CoreException
     */
    private void moveTempFile( File tempFile, File file ) throws CoreException
    {
        if( file.exists() )
        {
            if( !safeDelete( file, 2 ) )
            {
                // attempt to rewrite an existing file with the tempFile contents if
                // the existing file can't be deleted to permit the move
                try
                {
                    InputStream in = new FileInputStream( tempFile );
                    IStatus status = copyFile( in, file.getPath() );
                    if( !status.isOK() )
                    {
                        MultiStatus status2 =
                            new MultiStatus( ThemeCore.PLUGIN_ID, 0, NLS.bind(
                                Messages.errorDeleting, file.toString() ), null );
                        status2.add( status );
                        throw new CoreException( status2 );
                    }
                    return;
                }
                catch( FileNotFoundException e )
                {
                    // shouldn't occur
                }
                finally
                {
                    tempFile.delete();
                }
                /*
                 * if (!safeDelete(file, 8)) { tempFile.delete(); throw new CoreException(new Status(IStatus.ERROR,
                 * ThemeCore.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, file.toString()), null)); }
                 */
            }
        }
        if( !safeRename( tempFile, file, 10 ) )
            throw new CoreException( new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                Messages.errorRename, tempFile.toString() ), null ) );
    }

    /**
     * Copy a file from a to b. Closes the input stream after use.
     *
     * @param in
     *            an InputStream
     * @param to
     *            the file to copy to
     * @return a status
     */
    private IStatus copyFile( InputStream in, String to )
    {
        OutputStream out = null;

        try
        {
            out = new FileOutputStream( to );

            int avail = in.read( buf );
            while( avail > 0 )
            {
                out.write( buf, 0, avail );
                avail = in.read( buf );
            }
            return Status.OK_STATUS;
        }
        catch( Exception e )
        {
            ThemeCore.logError( "Error copying file", e );
            return new Status( IStatus.ERROR, ThemeCore.PLUGIN_ID, 0, NLS.bind(
                Messages.errorCopyingFile, new String[] { to, e.getLocalizedMessage() } ), e );
        }
        finally
        {
            try
            {
                if( in != null )
                    in.close();
            }
            catch( Exception ex )
            {
                // ignore
            }
            try
            {
                if( out != null )
                    out.close();
            }
            catch( Exception ex )
            {
                // ignore
            }
        }
    }

    /**
     * Safe delete. Tries to delete multiple times before giving up.
     *
     * @param f
     * @return <code>true</code> if it succeeds, <code>false</code> otherwise
     */
    private static boolean safeDelete( File f, int retrys )
    {
        int count = 0;
        while( count < retrys )
        {
            if( !f.exists() )
                return true;

            f.delete();

            if( !f.exists() )
                return true;

            count++;
            // delay if we are going to try again
            if( count < retrys )
            {
                try
                {
                    Thread.sleep( 100 );
                }
                catch( Exception e )
                {
                    // ignore
                }
            }
        }
        return false;
    }

    /**
     * Safe rename. Will try multiple times before giving up.
     *
     * @param from
     * @param to
     * @param retrys
     *            number of times to retry
     * @return <code>true</code> if it succeeds, <code>false</code> otherwise
     */
    private static boolean safeRename( File from, File to, int retrys )
    {
        // make sure parent dir exists
        File dir = to.getParentFile();
        if( dir != null && !dir.exists() )
            dir.mkdirs();

        int count = 0;
        while( count < retrys )
        {
            if( from.renameTo( to ) )
                return true;

            count++;
            // delay if we are going to try again
            if( count < retrys )
            {
                try
                {
                    Thread.sleep( 100 );
                }
                catch( Exception e )
                {
                    // ignore
                }
            }
        }
        return false;
    }

    private static void addArrayToList( List<IStatus> list, IStatus[] a )
    {
        if( list == null || a == null || a.length == 0 )
            return;

        int size = a.length;
        for( int i = 0; i < size; i++ )
            list.add( a[i] );
    }

}
