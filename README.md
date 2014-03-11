gtfs-rt-admin
===========

A web-based framework for managing GTFS-realtime service alerts.  Supports multiple agencies in single deployment. 

*Requires*

Play Framework 1.2.5
Postgresql 9.1+ 
PostGIS 1.5+

*Install*

Create PostGIS enabled database. Edit "db.url" in conf/applications.conf to reference database.

Add zipped GTFS data at `data/gtfs.zip`

Type `play run` from the command prompt.