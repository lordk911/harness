package com.actionml.entity

/**
  *
  * <a href="http://predictionio.incubator.apache.org/datacollection/eventapi/">http://predictionio.incubator.apache.org/datacollection/eventapi/</a>
  * @author The ActionML Team (<a href="http://actionml.com">http://actionml.com</a>)
  * 28.01.17 12:14
  */
case class Event(
  // Name of the event.
  // (Examples: "sign-up", "rate", "view", "buy").
  // Note: All event names start with "$" and "pio_" are reserved
  // and shouldn't be used as your custom event name (eg. "$set").
  event: String,

  // The entity type. It is the namespace of the entityId
  // and analogous to the table name of a relational database.
  // The entityId must be unique within same entityType.
  // Note: All entityType names start with "$" and "pio_" are reserved and shouldn't be used.
  entityType: String,

  // The entity ID. entityType-entityId becomes the unique identifier of the entity.
  // For example, you may have entityType named user, and different entity IDs,
  // say 1 and 2. In this case, user-1 and user-2 uniquely identifies entities.
  entityId: String,

  // (Optional) The target entity type.
  // Note: All entityType names start with "$" and "pio_" are reserved and shouldn't be used.
  targetEntityType: Option[String] = None,

  // (Optional) The target entity ID.
  targetEntityId: Option[String] = None,

  // (Optional) See Note About Properties below
  // Note: All peroperty names start with "$" and "pio_" are reserved
  // and shouldn't be used as keys inside properties.
  properties: Option[Map[String, String]] = None,

  // (Optional) The time of the event.
  // Although Event Server's current system time and UTC timezone will be used if this is unspecified,
  // it is highly recommended that this time should be generated by the client application in order to accurately record the time of the event.
  // Must be in ISO 8601 format (e.g.2004-12-13T21:39:45.618Z, or 2014-09-09T16:17:42.937-08:00).
  eventTime: Option[String] = None
)