/**
 * (c) 2011, Alejandro Serrano
 * Released under the terms of the EPL.
 */
package net.sf.eclipsefp.haskell.ui.internal.editors.cabal.forms.stanzas;

import net.sf.eclipsefp.haskell.ui.internal.editors.cabal.forms.FormEntryMultiSelect;
import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * Form entry that wraps the serialization of the 'main-is' property.
 * @author Alejandro Serrano
 *
 */
public class MainIsFormEntry extends FormEntryMultiSelect {

  public MainIsFormEntry( final ITreeContentProvider contents ) {
    super( contents, true );
  }

  @Override
  public String getValue() {
    String value = super.getValue();
    return value.length()==0 ? "" : value.replace( '.', '/' ).concat( ".hs" );
  }

  @Override
  public void setValue( final String value, final boolean blockNotification ) {
    String realValue = (value == null) ? "" : value;

    if (realValue.trim().equals( getValue().trim() )) {
      return;
    }

    if (!realValue.endsWith( ".hs" )) {
      super.setValue( realValue, blockNotification );
    } else {
      String newValue = realValue.substring( 0, realValue.length() - 3 ).replace( '/', '.' );
      super.setValue( newValue, blockNotification );
    }
  }

}
