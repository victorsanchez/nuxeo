/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.admin.permissions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Work archiving ACEs based on a query.
 *
 * @since 7.4
 */
public class PermissionsPurgeWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_BATCH_SIZE = 20;

    public static final String CATEGORY = "permissionsPurge";

    protected DocumentModel searchDocument;

    protected int batchSize = DEFAULT_BATCH_SIZE;

    public PermissionsPurgeWork(DocumentModel searchDocument) {
        this.searchDocument = searchDocument;
    }

    @Override
    public String getTitle() {
        return String.format("Permissions purge for '%s' user and %s document ids",
                searchDocument.getPropertyValue("rs:ace_username"),
                StringUtils.join((List<String>) searchDocument.getPropertyValue("rs:ecm_ancestorIds"), ","));
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public void work() {
        setStatus("Purging");
        openSystemSession();

        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition def = pageProviderService.getPageProviderDefinition("permissions_purge");
        String query = NXQLQueryBuilder.getQuery(searchDocument, def.getWhereClause(), null);

        IterableQueryResult result = session.queryAndFetch(query, NXQL.NXQL);
        List<String> docIds = new ArrayList<>();
        try {
            for (Map<String, Serializable> map : result) {
                docIds.add((String) map.get("ecm:uuid"));
            }
        } finally {
            result.close();
        }

        List<String> usernames = (List<String>) searchDocument.getPropertyValue("rs:ace_username");
        int acpUpdatedCount = 0;
        for (String docId : docIds) {
            DocumentRef ref = new IdRef(docId);
            ACP acp = session.getACP(ref);
            // cleanup acp for all principals
            boolean changed = false;
            for (String username : usernames) {
                for (ACL acl : acp.getACLs()) {
                    for (ACE ace : acl) {
                        if (username.equals(ace.getUsername())) {
                            Calendar now = new GregorianCalendar();
                            ace.setEnd(now);
                            changed = true;
                        }
                    }
                }
            }

            try {
                if (changed) {
                    session.setACP(ref, acp, true);
                    acpUpdatedCount++;
                    if (acpUpdatedCount % batchSize == 0) {
                        commitOrRollbackTransaction();
                        startTransaction();
                    }
                }
            } catch (TransactionRuntimeException e) {
                if (e.getMessage().contains("Transaction timeout")) {
                    batchSize = 1;
                }
                throw e;
            }

        }
        setStatus(null);
    }

    @Override
    public int getRetryCount() {
        return 10;
    }
}
