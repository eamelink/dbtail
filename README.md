dbtail
======

Db tail is a quick and dirty attempt at providing an alternative for `tail -f`, but for an append-only database table.

Current limitations
-------------------

 * MySQL only
 * Expects an 'id' field to be primary key

Usage
-----

Easiest with an example:

    sbt "run -h localhost -u foo -p bar -d databasename select * from logs where account_id = 5"

This will connect to the database and execute the query with an appended `ORDER BY id ASC LIMIT 10`. It will figure out the latest value for the `id` column, and run queries that search for newer records