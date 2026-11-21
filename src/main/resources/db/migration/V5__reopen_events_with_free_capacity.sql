-- Recover future events left sold out after a cancellation in older versions.
UPDATE events AS event
SET status = 'PUBLISHED'
FROM venues AS venue
WHERE venue.id = event.venue_id
  AND event.status = 'SOLD_OUT'
  AND event.event_date > LOCALTIMESTAMP
  AND EXISTS (
      SELECT 1
      FROM tickets AS ticket
      WHERE ticket.event_id = event.id AND ticket.status = 'CANCELLED'
  )
  AND (
      SELECT COUNT(*)
      FROM tickets AS ticket
      WHERE ticket.event_id = event.id AND ticket.status IN ('PAID', 'USED')
  ) < venue.capacity;
