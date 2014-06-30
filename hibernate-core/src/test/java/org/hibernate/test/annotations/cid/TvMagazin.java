//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AssociationOverrides({
@AssociationOverride(name = "id.channel", joinColumns = @JoinColumn(name = "chan_id", nullable = false)),
@AssociationOverride(name = "id.presenter", joinColumns = @JoinColumn(name = "presenter_name", nullable = false))})
public class TvMagazin {
	@EmbeddedId
	public TvMagazinPk id;
	@Temporal(TemporalType.TIME)
    @Column(name="`time`")
	Date time;
}
