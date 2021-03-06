// Copyright (c) 2006-2008 by Leif Frenzel - see http://leiffrenzel.de
// This code is made available under the terms of the Eclipse Public License,
// version 1.0 (EPL). See http://www.eclipse.org/legal/epl-v10.html
package net.sf.eclipsefp.haskell.ui.internal.preferences.hsimpls;


import net.sf.eclipsefp.haskell.core.compiler.HsImplementation;
import net.sf.eclipsefp.haskell.core.compiler.HsImplementationType;
import net.sf.eclipsefp.haskell.ui.HaskellUIPlugin;
import net.sf.eclipsefp.haskell.ui.internal.util.UITexts;
import net.sf.eclipsefp.haskell.ui.util.DefaultStatus;
import net.sf.eclipsefp.haskell.ui.util.SWTUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/** <p>Dialog for the user to add another Haskell implementation.</p>
  *
  * @author Leif Frenzel
  */
public class HsImplementationDialog extends StatusDialog {

  private static final String DIALOG_SETTINGS_ID
    = HsImplementationDialog.class.getName();
	private static final String KEY_DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$
  private static final String KEY_DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$

	private Combo cmbImplementationType;
	private Text txtName;
	private Text txtBinFolder;
  private final ImplementationsBlock implementationsBlock;

  private final HsImplementation origImpl;
  private final HsImplementation currentImpl;
  private Label lblVersion;
  private Label lblLibDir;

	HsImplementationDialog( final Shell shell,
	                        final ImplementationsBlock implementationsBlock,
	                        final HsImplementation impl ) {
    super( shell );
    setShellStyle( getShellStyle() | SWT.RESIZE );
    this.origImpl=impl;
		this.implementationsBlock = implementationsBlock;
		currentImpl = new HsImplementation();
		if( impl != null ) {
		  currentImpl.setName( impl.getName() );
      currentImpl.setBinDir( impl.getBinDir() );
		}
		currentImpl.setType( HsImplementationType.GHC );
	}

	HsImplementation getResult() {
	  return currentImpl;
	}


	// interface methods of StatusDialog
	////////////////////////////////////

  @Override
  protected Control createDialogArea( final Composite parent ) {
    Composite composite = ( Composite )super.createDialogArea( parent );
    ( ( GridLayout )composite.getLayout() ).numColumns = 3;

    SWTUtil.createLabel( composite, UITexts.hsImplementationDialog_type, 1 );
	  String[] types = new String[] { HsImplementationType.GHC.toString() };
    cmbImplementationType = createCombo( composite, types );
    SWTUtil.createLabel( composite, UITexts.hsImplementationDialog_name, 1 );
    txtName = SWTUtil.createSingleText( composite, 2 );
    SWTUtil.createLabel( composite, UITexts.hsImplementationDialog_binDir, 1 );
    txtBinFolder = SWTUtil.createSingleText( composite, 1 );
    createBrowseButton( composite );
    SWTUtil.createLabel( composite, UITexts.hsImplementationDialog_version, 1 );
    lblVersion = SWTUtil.createLabel( composite, "", 2 ); //$NON-NLS-1$
    SWTUtil.createLabel( composite, UITexts.hsImplementationDialog_libDir, 1 );
    lblLibDir = SWTUtil.createLabel( composite, "", 2 ); //$NON-NLS-1$

    initializeFields();
    txtName.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( final ModifyEvent evt ) {
        currentImpl.setName( txtName.getText() );
        validate();
        updateFields();
      }
    } );
    txtBinFolder.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( final ModifyEvent evt ) {
        currentImpl.setBinDir( txtBinFolder.getText() );
        validate();
        updateFields();
      }
    } );
    applyDialogFont( composite );
    return composite;
  }

	@Override
  protected void updateButtonsEnableState( final IStatus status ) {
	  Button ok = getButton( IDialogConstants.OK_ID );
	  if( ok != null && !ok.isDisposed() ) {
	    ok.setEnabled( status.getSeverity() == IStatus.OK );
	  }
	}

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings settings = HaskellUIPlugin.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection( DIALOG_SETTINGS_ID );
    if( section == null ) {
      section = settings.addNewSection( DIALOG_SETTINGS_ID );
    }
    return section;
  }

  @Override
  protected Point getInitialSize() {
    IDialogSettings settings = getDialogBoundsSettings();
    if( settings != null ) {
      try {
        int width = settings.getInt( KEY_DIALOG_WIDTH );
        int height = settings.getInt( KEY_DIALOG_HEIGHT );
        if( width > 0 & height > 0 ) {
          return new Point( width, height );
        }
      } catch( NumberFormatException nfe ) {
        return new Point( 500, 570 );
      }
    }
    return new Point( 500, 570 );
  }

  @Override
  public void create() {
    super.create();
    txtName.setFocus();
  }


  // helping functions
  ////////////////////

  private void validate() {
    if( implementationsBlock.isDuplicateName( txtName.getText(),origImpl ) ) {
      String msg = UITexts.hsImplementationDialog_duplicate;
      DefaultStatus status = new DefaultStatus();
      status.setError( NLS.bind( msg, new String[] { txtName.getText() } ) );
      updateStatus( status );
    } else {
      IStatus[] statuss = currentImpl.validate();
      IStatus max = null;
      for( int i = 0; i < statuss.length; i++ ) {
        IStatus curr = statuss[ i ];
        if( curr.matches( IStatus.ERROR ) ) {
          updateStatus( curr );
          return;
        }
        if( max == null || curr.getSeverity() > max.getSeverity() ) {
          max = curr;
        }
      }
      updateStatus( max );
    }
  }

  private void createBrowseButton( final Composite composite ) {
    String text = UITexts.hsImplementationDialog_btnBrowse;
    Button browse = SWTUtil.createPushButton( composite, text );
    browse.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( final SelectionEvent e ) {
        DirectoryDialog dialog = new DirectoryDialog( getShell() );
        dialog.setFilterPath( txtBinFolder.getText() );
        dialog.setMessage( UITexts.hsImplementationDialog_dlgBrowse );
        String newPath = dialog.open();
        if( newPath != null ) {
          txtBinFolder.setText( newPath );
        }
      }
    } );
  }

  private Combo createCombo( final Composite parent, final String[] items ) {
    Combo result = new Combo( parent, SWT.READ_ONLY );
    result.setFont( parent.getFont() );
    GridData gd = new GridData( GridData.FILL_HORIZONTAL );
    gd.horizontalSpan = 2;
    result.setLayoutData( gd );
    result.setItems( items );
    result.select( 0 );
    return result;
  }

  private void initializeFields() {
    String name = currentImpl.getName() == null ? "" : currentImpl.getName(); //$NON-NLS-1$
    String binDir = currentImpl.getBinDir() == null ? "" : currentImpl.getBinDir(); //$NON-NLS-1$
    txtName.setText( name );
    txtBinFolder.setText( binDir );
    cmbImplementationType.setEnabled( false );
    validate();
    updateFields();
  }

  private void updateFields() {
    String vs = currentImpl.getVersion();
    if( vs == null ) {
      vs = ""; //$NON-NLS-1$
    }
    lblVersion.setText( vs.trim() );
    String ld = currentImpl.getLibDir();
    if( ld == null ) {
      ld = ""; //$NON-NLS-1$
    }
    lblLibDir.setText( ld.trim() );

    if (vs.length()>0 && (currentImpl.getName()==null ||currentImpl.getName().length()==0)){
      String nameStub=NLS.bind( UITexts.hsImplementationDialog_name_default,currentImpl.getType().toString(),vs);
      txtName.setText(nameStub);
      int index=1;
      while( implementationsBlock.isDuplicateName( txtName.getText(),origImpl ) ) {
        txtName.setText(NLS.bind(UITexts.hsImplementationDialog_name_index,nameStub,String.valueOf(index)));
        index++;
      }
      txtName.notifyListeners( SWT.Modify, new Event() );
    }
  }
}
