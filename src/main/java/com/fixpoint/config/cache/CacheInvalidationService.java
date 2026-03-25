package com.fixpoint.config.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final CacheManager cacheManager;

    public void evictDashboard() {
        clear(
                CacheNames.DASHBOARD_SUMMARY,
                CacheNames.DASHBOARD_STORAGE
        );
    }

    public void evictTickets() {
        clear(
                CacheNames.TICKETS_ALL,
                CacheNames.TICKET_BY_ID,
                CacheNames.TICKETS_BY_CLIENT,
                CacheNames.TICKETS_BY_STATUS,
                CacheNames.TICKET_STATUS_DEFINITIONS
        );
    }

    public void evictClients() {
        clear(
                CacheNames.CLIENTS_ALL,
                CacheNames.CLIENT_BY_ID,
                CacheNames.CLIENTS_BY_NAME
        );
    }

    public void evictInventory() {
        clear(
                CacheNames.INVENTORY_ALL,
                CacheNames.INVENTORY_BY_ID
        );
    }

    public void evictAttachments() {
        clear(
                CacheNames.ATTACHMENTS_BY_TICKET,
                CacheNames.ATTACHMENT_BY_ID,
                CacheNames.RECENT_ATTACHMENTS
        );
    }

    private void clear(String... cacheNames) {
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
