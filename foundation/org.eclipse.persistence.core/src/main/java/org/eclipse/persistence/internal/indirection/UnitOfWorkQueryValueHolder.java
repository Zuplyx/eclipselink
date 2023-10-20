/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
package org.eclipse.persistence.internal.indirection;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.indirection.IndirectContainer;
import org.eclipse.persistence.indirection.ValueHolderInterface;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.internal.sessions.remote.RemoteValueHolder;
import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.ForeignReferenceMapping;
import org.eclipse.persistence.queries.ObjectBuildingQuery;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadQuery;

import java.util.Collection;

/**
 * UnitOfWorkQueryValueHolder either wraps a database-stored object and
 * implements behavior to access it. The object is read from
 * the database by invoking a user-specified query.
 * <p>
 * Alternatively UnitOfWorkQueryValueHolder can be directly created from a query
 * without the overhead of additional wrapping. In this case it will save the
 * original value read from the database in {@link #originalValue} and return
 * a clone. Note that in this case, the original object and the clone will share
 * the same UnitOfWorkQueryValueHolder. Therefore, changes have to be calculated
 * by comparing {@link #originalValue} to {@link #value}.</p>
 * <p>
 * TODO: Change detection, as described above, still has to be implemented!
 * This class serves as a <b>POC</b>, change detection is currently
 * <b>not</b> functional! </p>
 * <p>
 * This value holder is used only in the unit of work.</p>
 *
 * @author    Sati
 */
public class UnitOfWorkQueryValueHolder<T> extends UnitOfWorkValueHolder<T> {

    /**
     * Stores the query to be executed.
     */
    protected transient ReadQuery query;
    protected transient Object sourceObject;
    protected Integer refreshCascade;

    /**Stores the original value read from the DB. Used for change detection.*/
    protected volatile T originalValue;

    /**
     * Create a UnitOfWorkQueryValueHolder from a QueryBasedValueHolder.
     * If the valueHolder is a subclass of QueryBasedValueHolder, the resulting
     * UnitOfWorkQueryValueHolder will wrap it. Otherwise, it will copy query,
     * sourceObject, row and, if instantiated, value into a new non-wrapping
     * UnitOfWorkQueryValueHolder.
     *
     * @param valueHolder
     * @param session
     * @param clone
     * @param row
     * @param mapping
     * @return
     * @param <T>
     */
    public static <T> UnitOfWorkQueryValueHolder<T> fromQueryBasedValueHolder(QueryBasedValueHolder<T> valueHolder, AbstractSession session, Object clone, AbstractRecord row, ForeignReferenceMapping mapping) {
        UnitOfWorkQueryValueHolder<T> result;
        if (valueHolder.getClass() == QueryBasedValueHolder.class) {
            result = new UnitOfWorkQueryValueHolder<>(valueHolder, session);
        } else {
            result = new UnitOfWorkQueryValueHolder<>(valueHolder, clone, mapping, row, (UnitOfWorkImpl) session);
        }
        return result;
    }

    /**
     * Create a UnitOfWorkQueryValueHolder from a QueryBasedValueHolder.
     *
     * This should never be called for subclasses of QueryBasedValueHolder!
     *
     * @param valueHolder
     * @param session
     */
    private UnitOfWorkQueryValueHolder(QueryBasedValueHolder<T> valueHolder, AbstractSession session){
        this (valueHolder.query, valueHolder.sourceObject, valueHolder.row, session);
        if (valueHolder.isInstantiated) {
            // copy value and set this as instantiated
            value = valueHolder.value;
            originalValue = buildCloneFor(value);
            isInstantiated = true;
        }
    }

    /**
     * Initialize the query-based value holder.
     */
    public UnitOfWorkQueryValueHolder(ReadQuery query, AbstractRecord row, AbstractSession session) {
        this (query, null, row, session);
    }

    /**
     * Initialize the query-based value holder.
     */
    public UnitOfWorkQueryValueHolder(ReadQuery query, Object sourceObject, AbstractRecord row, AbstractSession session) {
        this.row = row;
        this.session = session;

        // Make sure not to put a ClientSession or IsolatedClientSession in
        // the shared cache (indirectly).
        // Skip this if unitOfWork, for we use session.isUnitOfWork() to
        // implement
        // isTransactionalValueholder(), saving us from needing a boolean
        // instance variable.
        // If unitOfWork this safety measure is deferred until merge time with
        // releaseWrappedValuehHolder.
        // Note that if isolated session & query will return itself, which is
        // safe
        // for if isolated it will not go in the shared cache.
        if (!session.isUnitOfWork()) {
            this.session = session.getRootSession(query);
        }
        this.query = query;
        this.sourceObject = sourceObject;
        this.wrappedValueHolder = this;
    }

    /**
     * INTERNAL:
     * Returns the refresh cascade policy that was set on the query that was used to instantiate the valueholder
     * a null value means that a non refresh query was used.
     */
    public Integer getRefreshCascadePolicy(){
        return this.refreshCascade;
    }

    /**
     * Process against the UOW and attempt to load a local copy before going to the shared cache
     * If null is returned then the calling UOW will instantiate as normal.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public T getValue(UnitOfWorkImpl uow) {
        if(query == null) return super.getValue();
        if (this.query.isReadObjectQuery()){
            return (T) this.query.getQueryMechanism().checkCacheForObject(this.query.getTranslationRow(), uow);
        }
        //not able to shortcircuit cache lookup to UOW return null;
        return null;
    }

    /**
     * Return the query. The query for a QueryBasedValueHolder is constructed
     * dynamically based on the original query on the parent object and the
     * mapping configuration. If modifying a query the developer must be careful
     * not to change the results returned as that may cause the application to
     * see incorrect results.
     */
    public ReadQuery getQuery() {
        return query;
    }

    @Override
    public boolean shouldAllowInstantiationDeferral() {
        // in wrapped mode -> use UnitOfWorkValueHolder implementation
        if (wrappedValueHolder != this) {
            return super.shouldAllowInstantiationDeferral();
        }
        // DatabaseValueHolder implementation (as inherited by QueryBasedValueHolder)
        return true;
    }

    @Override
    protected T instantiate() throws DatabaseException {
        // UnitOfWorkValueHolder behaviour
        if(query == null) return super.instantiate();
        // QueryBasedValueHolder behaviour
        T value = instantiate(session);
        if(mapping != null) { // TODO: correct condition?
            /* we need to return a clone, so change detection via originalValue
             * works. otherwise changes to value will also affect originalValue */
            return buildCloneFor(value);
        }
        return value;
    }

    /**
     * INTERNAL:
     * Get the original, unmodified value read from the DB for change detection.
     *
     * @return the original value read from the DB before any modifications
     */
    public T getOriginalValue() {
        return originalValue;
    }

    /**
     * Instantiate the object by executing the query on the session.
     */
    @SuppressWarnings({"unchecked"})
    protected T instantiate(AbstractSession session) throws DatabaseException {
        if (session == null) {
            throw ValidationException.instantiatingValueholderWithNullSession();
        }
        if (this.query.isObjectBuildingQuery() && ((ObjectBuildingQuery)this.query).shouldRefreshIdentityMapResult()){
            this.refreshCascade = this.query.getCascadePolicy();
        }
        T result = (T) session.executeQuery(getQuery(), getRow());
        // save the original value read from the DB for later change detection
        originalValue = result;
        // Bug 489898 - ensure that the query's session is dereferenced, post-execution
        getQuery().setSession(null);
        return  result;
    }

    /**
     * Triggers UnitOfWork valueholders directly without triggering the wrapped
     * valueholder (this).
     * <p>
     * When in transaction and/or for pessimistic locking the
     * UnitOfWorkValueHolder needs to be triggered directly without triggering
     * the wrapped valueholder. However only the wrapped valueholder knows how
     * to trigger the indirection, i.e. it may be a batchValueHolder, and it
     * stores all the info like the row and the query. Note: This method is not
     * thread-safe. It must be used in a synchronized manner
     */
    @Override
    public T instantiateForUnitOfWorkValueHolder(UnitOfWorkValueHolder<T> unitOfWorkValueHolder) {
        return instantiate(unitOfWorkValueHolder.getUnitOfWork());
    }

    /**
     * INTERNAL:
     * Run any extra code required after the valueholder instantiates
     * For query based VH, we notify the cache that the valueholder has been triggered
     */
    @Override
    public void postInstantiate(){
        if(query != null) {
            DatabaseMapping mapping = query.getSourceMapping();
            if (mapping != null && mapping.isForeignReferenceMapping()) {
                // Fix for Bug#474232
                ClassDescriptor descriptor = mapping.getDescriptor();
                final IndirectionPolicy indirectionPolicy = ((ForeignReferenceMapping) mapping).getIndirectionPolicy();
                if (indirectionPolicy != null && indirectionPolicy.isWeavedObjectBasicIndirectionPolicy()) {
                    if (!isCoordinatedWithProperty && mapping.isObjectReferenceMapping() && sourceObject != null && value != null) {
                        // Bug#487930
                        Object object = sourceObject;
                        if (descriptor != null && descriptor.isAggregateDescriptor()) {
                            // navigate to the leaf element in the accessor tree
                            for (AttributeAccessor accessor : descriptor.getAccessorTree()) {
                                object = accessor.getAttributeValueFromObject(object);
                            }
                        }
                        mapping.setAttributeValueInObject(object, this);
                        isCoordinatedWithProperty = true;
                    }
                }

                if (descriptor == null || descriptor.isAggregateDescriptor()) {
                    descriptor = session.getDescriptor(sourceObject);
                }
                if (descriptor != null) {
                    session.getIdentityMapAccessorInstance().getIdentityMap(descriptor).lazyRelationshipLoaded(sourceObject, this, (ForeignReferenceMapping) query.getSourceMapping());
                }
            }
        }
    }

    /**
     * Releases a wrapped valueholder privately owned by a particular unit of
     * work.
     * <p>
     * When unit of work clones are built directly from rows no object in the
     * shared cache points to this valueholder, so it can store the unit of work
     * as its session. However once that UnitOfWork commits and the valueholder
     * is merged into the shared cache, the session needs to be reset to the
     * root session, ie. the server session.
     */
    @Override
    public void releaseWrappedValueHolder(AbstractSession targetSession) {
        AbstractSession session = getSession();
        if ((session != null) && session.isUnitOfWork()) {
            setSession(targetSession);
        }
    }

    /**
     * Reset all the fields that are not needed after instantiation.
     */
    @Override
    protected void resetFields() {
        super.resetFields();
        this.query = null;
    }

    /**
     * Set the query.
     */
    protected void setQuery(ReadQuery theQuery) {
        query = theQuery;
    }

    /**
     * INTERNAL: Answers if this valueholder is a pessimistic locking one. Such
     * valueholders are special in that they can be triggered multiple times by
     * different UnitsOfWork. Each time a lock query will be issued. Hence even
     * if instantiated it may have to be instantiated again, and once
     * instantiated all fields can not be reset.
     * <p>
     * Since locks will be issued each time this valueholder is triggered,
     * triggering this directly on the session in auto commit mode will generate
     * an exception. This only UnitOfWorkValueHolder's wrapping this can trigger
     * it. Note: This method is not thread-safe. It must be used in a
     * synchronized manner
     */
    @Override
    public boolean isPessimisticLockingValueHolder() {
        // wrapped scenario -> use UnitOfWorkValueHolder implementation
        if(wrappedValueHolder != this) return super.isPessimisticLockingValueHolder();
        // Get the easy checks out of the way first.
        if ((this.query == null) || !this.query.isObjectLevelReadQuery()) {
            return false;
        }
        ObjectLevelReadQuery query = (ObjectLevelReadQuery) this.query;

        // Note even if the reference class is not locked, but the valueholder
        // query has joined attributes, then this may count as a lock query.
        // This means it is possible to trigger a valueholder to get an object
        // which is not to be pess. locked and get an exception for triggering
        // it on the session outside a transaction.
        return query.isLockQuery(this.session);
    }

    public void setSourceObject(Object sourceObject) {
        this.sourceObject = sourceObject;
    }

    /**
     * INTERNAL:
     */
    public void setRefreshCascadePolicy(Integer refreshCascadePolicy) {
        this.refreshCascade = refreshCascadePolicy;
    }

    /**
     * Register this ValueHolder for the given UnitOfWork.
     * This method is provided as an alternative for the
     * {@link #UnitOfWorkQueryValueHolder(ValueHolderInterface, Object, DatabaseMapping, UnitOfWorkImpl)
     * constructor} to avoid wrapping.
     *
     * @param clone
     * @param mapping
     * @param unitOfWork
     */
    public void registerForUnitOfWork(Object clone, DatabaseMapping mapping, UnitOfWorkImpl unitOfWork) {
        this.mapping = mapping;
        this.session = unitOfWork;
        this.sourceAttributeName = mapping.getAttributeName();
        this.relationshipSourceObject = clone;

        if (unitOfWork.isRemoteUnitOfWork()) {
            if (this.wrappedValueHolder instanceof RemoteValueHolder) {
                this.wrappedValueHolderRemoteID = ((RemoteValueHolder<T>)this.wrappedValueHolder).getID();
            }
            this.remoteUnitOfWork = unitOfWork;
            this.sourceObject = clone;
        }
    }
    protected UnitOfWorkQueryValueHolder() {
        super();
    }

    @Override
    public boolean isEasilyInstantiated() {
        // not wrapped -> use DatabaseValueHolder implementation
        if(query != null || wrappedValueHolder == this) return isInstantiated;
        return super.isEasilyInstantiated();
    }

    protected UnitOfWorkQueryValueHolder(ValueHolderInterface<T> attributeValue, Object clone, DatabaseMapping mapping, UnitOfWorkImpl unitOfWork) {
        super(attributeValue, clone, mapping, unitOfWork);
    }

    public UnitOfWorkQueryValueHolder(ValueHolderInterface<T> attributeValue, Object clone, ForeignReferenceMapping mapping, AbstractRecord row, UnitOfWorkImpl unitOfWork) {
        this(attributeValue, clone, mapping, unitOfWork);
        this.row = row;
    }

    /**
     * Backup the clone attribute value.
     */
    @Override
    protected Object buildBackupCloneFor(Object cloneAttributeValue) {
        return this.mapping.buildBackupCloneForPartObject(cloneAttributeValue, null, null, getUnitOfWork());
    }

    /**
     * Clone the original attribute value.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public T buildCloneFor(Object originalAttributeValue) {
        Integer refreshCascade = this.refreshCascade;
        if (wrappedValueHolder instanceof QueryBasedValueHolder){
            refreshCascade = ((QueryBasedValueHolder<?>)getWrappedValueHolder()).getRefreshCascadePolicy();
        }
        T clone = (T) this.mapping.buildCloneForPartObject(originalAttributeValue, null, null, this.relationshipSourceObject, getUnitOfWork(), refreshCascade, true, true);
        // Bug 414801
        if (wrappedValueHolder.isInstantiated() && refreshCascade != null) {
            if (wrappedValueHolder instanceof QueryBasedValueHolder) {
                ((QueryBasedValueHolder<T>) getWrappedValueHolder()).setRefreshCascadePolicy(null);
            }
            else {
                setRefreshCascadePolicy(null);
            }
        }
        return clone;
    }

    /**
     * Ensure that the backup value holder is populated.
     */
    @Override
    public void setValue(T theValue) {
        // Must force instantiation to be able to compare with the old value.
        if (!this.isInstantiated) {
            instantiate();
        }
        T oldValue = getValue();
        super.setValue(theValue);
        updateForeignReferenceSet(theValue, oldValue);
    }

    /**
     * INTERNAL:
     * Here we now must check for bi-directional relationship.
     * If the mapping has a relationship partner then we must maintain the original relationship.
     * We only worry about ObjectReferenceMappings as the collections mappings will be handled by transparentIndirection
     */
    public void updateForeignReferenceRemove(Object value) {
        DatabaseMapping sourceMapping = this.getMapping();
        if (sourceMapping == null) {
            //mapping is a transient attribute. If it does not exist then we have been serialized
            return;
        }

        if (sourceMapping.isPrivateOwned()) {
            // don't null out backpointer on private owned relationship because it will cause an
            // extra update.
            return;
        }

        //    ForeignReferenceMapping partner = (ForeignReferenceMapping)getMapping().getRelationshipPartner();
        ForeignReferenceMapping partner = this.getRelationshipPartnerFor(value);
        if (partner != null) {
            if (value != null) {
                Object unwrappedValue = partner.getDescriptor().getObjectBuilder().unwrapObject(value, getSession());
                Object oldParent = partner.getRealAttributeValueFromObject(unwrappedValue, getSession());
                Object sourceObject = getRelationshipSourceObject();

                if (oldParent == null) {
                    // value has already been set
                    return;
                }
                // PERF: If the collection is not instantiated, then do not instantiated it.
                if (partner.isCollectionMapping()) {
                    if ((!(oldParent instanceof IndirectContainer)) || ((IndirectContainer<?>)oldParent).isInstantiated()) {
                        if (!partner.getContainerPolicy().contains(sourceObject, oldParent, getSession())) {
                            // value has already been set
                            return;
                        }
                    }
                }

                if (partner.isObjectReferenceMapping()) {
                    // Check if it's already been set to null
                    partner.setRealAttributeValueInObject(unwrappedValue, null);
                } else if (partner.isCollectionMapping()) {
                    // If it is not in the collection then it has already been removed.
                    partner.getContainerPolicy().removeFrom(sourceObject, oldParent, getSession());
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Here we now must check for bi-directional relationship.
     * If the mapping has a relationship partner then we must maintain the original relationship.
     * We only worry about ObjectReferenceMappings as the collections mappings will be handled by transparentIndirection
     */
    public void updateForeignReferenceSet(Object value, Object oldValue) {
        if (value instanceof Collection) {
            //I'm passing a collection into the valueholder not an object
            return;
        }
        if (getMapping() == null) {
            //mapping is a transient attribute. If it does not exist then we have been serialized
            return;
        }

        //    ForeignReferenceMapping partner = (ForeignReferenceMapping)getMapping().getRelationshipPartner();
        ForeignReferenceMapping partner = this.getRelationshipPartnerFor(value);
        if (partner != null) {
            if (value != null) {
                Object unwrappedValue = partner.getDescriptor().getObjectBuilder().unwrapObject(value, getSession());
                Object oldParent = partner.getRealAttributeValueFromObject(unwrappedValue, getSession());
                Object sourceObject = getRelationshipSourceObject();
                Object wrappedSource = getMapping().getDescriptor().getObjectBuilder().wrapObject(sourceObject, getSession());

                if (oldParent == sourceObject) {
                    // value has already been set
                    return;
                }
                // PERF: If the collection is not instantiated, then do not instantiated it.
                if (partner.isCollectionMapping()) {
                    if ((!(oldParent instanceof IndirectContainer)) || ((IndirectContainer<?>)oldParent).isInstantiated()) {
                        if (partner.getContainerPolicy().contains(sourceObject, oldParent, getSession())) {
                            // value has already been set
                            return;
                        }
                    }
                }

                // Set the Object that was referencing this value to reference null, or remove value from its collection
                if (oldParent != null) {
                    if (getMapping().isObjectReferenceMapping()) {
                        if (!partner.isCollectionMapping()) {
                            // If the back pointer is a collection it's OK that I'm adding myself into the collection
                            getMapping().setRealAttributeValueInObject(oldParent, null);
                        }
                    } else if (getMapping().isCollectionMapping() && (!partner.isManyToManyMapping())) {
                        getMapping().getContainerPolicy().removeFrom(unwrappedValue, getMapping().getRealAttributeValueFromObject(oldParent, getSession()), getSession());
                    }
                }

                if (oldValue != null) {
                    // CR 3487
                    Object unwrappedOldValue = partner.getDescriptor().getObjectBuilder().unwrapObject(oldValue, getSession());

                    // if this object was referencing a different object reset the back pointer on that object
                    if (partner.isObjectReferenceMapping()) {
                        partner.setRealAttributeValueInObject(unwrappedOldValue, null);
                    } else if (partner.isCollectionMapping()) {
                        partner.getContainerPolicy().removeFrom(sourceObject, partner.getRealAttributeValueFromObject(unwrappedOldValue, getSession()), getSession());
                    }
                }

                // Now set the back reference of the value being passed in to point to this object
                if (partner.isObjectReferenceMapping()) {
                    partner.setRealAttributeValueInObject(unwrappedValue, wrappedSource);
                } else if (partner.isCollectionMapping()) {
                    partner.getContainerPolicy().addInto(wrappedSource, oldParent, getSession());
                }
            } else {
                updateForeignReferenceRemove(oldValue);
            }
        }
    }

    /**
     * Helper method to retrieve the relationship partner mapping.  This will take inheritance
     * into account and return the mapping associated with correct subclass if necessary.  This
     * is needed for EJB 2.0 inheritance
     */
    private ForeignReferenceMapping getRelationshipPartnerFor(Object partnerObject) {
        ForeignReferenceMapping partner = (ForeignReferenceMapping)getMapping().getRelationshipPartner();
        if ((partner == null) || (partnerObject == null)) {
            // no partner, nothing to do
            return partner;
        }

        // if the target object is not an instance of the class type associated with the partner
        // mapping, try and look up the same partner mapping but as part of the partnerObject's
        // descriptor.  Only check if inheritance is involved...
        if (partner.getDescriptor().hasInheritance()) {
            ClassDescriptor partnerObjectDescriptor = this.getSession().getDescriptor(partnerObject);
            if (!(partner.getDescriptor().getJavaClass().isAssignableFrom(partnerObjectDescriptor.getJavaClass()))) {
                return (ForeignReferenceMapping)partnerObjectDescriptor.getObjectBuilder().getMappingForAttributeName(partner.getAttributeName());
            }
        }
        return partner;
    }
}
