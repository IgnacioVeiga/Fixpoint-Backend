package com.fixpoint.config.cache;

public final class CacheNames {

    private CacheNames() {
    }

    public static final String DASHBOARD_SUMMARY = "dashboardSummary";
    public static final String DASHBOARD_STORAGE = "dashboardStorage";

    public static final String TICKETS_ALL = "ticketsAll";
    public static final String TICKET_BY_ID = "ticketById";
    public static final String TICKETS_BY_CLIENT = "ticketsByClient";
    public static final String TICKETS_BY_STATUS = "ticketsByStatus";
    public static final String TICKET_STATUS_DEFINITIONS = "ticketStatusDefinitions";

    public static final String CLIENTS_ALL = "clientsAll";
    public static final String CLIENT_BY_ID = "clientById";
    public static final String CLIENTS_BY_NAME = "clientsByName";

    public static final String INVENTORY_ALL = "inventoryAll";
    public static final String INVENTORY_BY_ID = "inventoryById";

    public static final String ATTACHMENTS_BY_TICKET = "attachmentsByTicket";
    public static final String ATTACHMENT_BY_ID = "attachmentById";
    public static final String RECENT_ATTACHMENTS = "recentAttachments";
}
