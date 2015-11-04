
-- TO BE DONE BEFORE RUNNING THIS SCRIPT
-- createuser first
-- GRANT ALL PRIVILEGES ON DATABASE mms to mmsuser;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO new_user;

-- drop DB at all cost
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'mms'
  AND pid <> pg_backend_pid();

-- Drop database if it exists
drop database if exists mms;

-- Create database
create database mms;

-- start using the database
\c mms;

-- Create tables
create table nodeTypes
(
  id serial primary key,
  name text not null
);

create table edgeTypes
(
  id serial primary key,
  name text not null
);

create table nodes
(
  id bigserial primary key,
  nodeRefId text not null unique,
  versionedRefId text not null,
  nodeType integer references nodeTypes(id) not null,
  sysmlId text not null unique
);

create table edges
(
  parent integer references nodes(id) not null,
  child integer references nodes(id) not null,
  edgeType integer references edgeTypes(id) not null,
  constraint unique_edges unique(parent, child, edgeType)
);

-- given two nodeRefId, insert an edge between the two
create or replace function insert_edge(text, text, text, integer)
  returns void as $$
  begin
    execute 'insert into ' || (format('edges%s', $3)) || ' values((select id from ' || format('nodes%s',$3) || ' 
      where sysmlId = || ' || $1 || '), (select id from ' || format('nodes%s', $3)   || ' where sysmlId = ' || $2 || '),  ' || $4 || ')';
  end;
$$ language plpgsql;

-- recursively get all children including oneself
create or replace function get_children(integer, integer, text, integer)
  returns table(id bigint) as $$
  begin
    return query
    execute '
    with recursive children(depth, nid, path, cycle) as (
      select 0 as depth, node.id from ' || format('nodes%s', $3) || ' node where node.id = ' || $1 || '
      union
      select (c.depth + 1) as depth, edge.child, path || cast(edge.child as bigint), edge.child = ANY(path)
        from ' || format('edges%s', $3) || ' edge, children c where edge.parent = nid and 
        edge.edgeType = ' || $2 || ' and not cycle
      )
      select distinct nid from children where depth <= ' || $4 || ';';
  end;
$$ language plpgsql;

create or replace function get_parents(integer, integer, text)
  returns table(id bigint, height integer) as $$
  begin
    return query
    execute '
    with recursive parents(height, nid, path, cycle) as (
    select 0, node.id, ARRAY[node.id], false from ' || format('nodes%s', $3) || ' node where node.id = ' || $1 || '
    union
      select (c.height + 1), edge.parent, path || cast(edge.parent as bigint), 
        edge.parent = ANY(path) from ' || format('edges%s', $3) || '
        edge, parents c where edge.child = nid and edge.edgeType = ' || $2 || '
        and not cycle 
      )
      select nid,height from parents order by height;';
  end;
$$ language plpgsql;

-- get all paths to a node
create type return_type as (pstart integer, pend integer, path integer[]);
create or replace function get_paths_to_node(integer, integer, text)
  returns setof return_type as $$
  begin
    return query
    execute '
    with recursive node_graph as (
      select parent as path_start, child as path_end,
             array[parent, child] as path 
      from ' || format('edges%s', $3) || ' where edgeType = ' || $2 || ' 
      union all
      select ng.path_start, nr.child as path_end,
           ng.path || nr.child as path
      from node_graph ng 
      join edges nr ON ng.path_end = nr.parent where nr.edgeType = ' || $2 || ' 
    ) 
    select * from node_graph where path_end = ' || $1 || ' order by path_start, array_length(path,1)';
  end;
$$ language plpgsql;
  
-- get all root parents of a node
create aggregate array_agg_mult(anyarray) (
    SFUNC = array_cat,
    STYPE = anyarray,
    INITCOND = '{}'
);

create or replace function array_sort_unique (anyarray)
  returns anyarray
  as $body$
    select array(
      select distinct $1[s.i]
      from generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
      order by 1
    );
  $body$
language sql;

insert into nodeTypes(name) values ('regular');
insert into nodeTypes(name) values ('document');

insert into edgeTypes(name) values ('regular');
insert into edgeTypes(name) values ('document');


GRANT ALL PRIVILEGES ON DATABASE mms to mmsuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mmsuser;
GRANT USAGE, SELECT ON SEQUENCE nodes_id_seq TO mmsuser;

-- test data etc. comment out when not in dev mode

/*
DO
$do$
BEGIN 
FOR i IN 1..250000 LOOP
  execute format('insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(%s, 1, (select nodeTypes.id from nodeTypes where name = ''regular''), %s)', i, i);
END LOOP;
END
$do$;


DO
$do$
BEGIN 
FOR i IN 1..250000 LOOP
  execute format('insert into edges values(%s, %s, 1)', floor(random() * 250000) + 1, floor(random()*250000) + 1);
END LOOP;
END
$do$;

insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values('1', 1, (select nodeTypes.id from nodeTypes where name = 'regular'), '1');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(2, 2, (select nodeTypes.id from nodeTypes where name = 'regular'), '2');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(3, 3, (select nodeTypes.id from nodeTypes where name = 'regular'), '3');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(4, 4, (select nodeTypes.id from nodeTypes where name = 'regular'), '4');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(5, 5, (select nodeTypes.id from nodeTypes where name = 'regular'), '5');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(6, 6, (select nodeTypes.id from nodeTypes where name = 'regular'), '6');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(7, 7, (select nodeTypes.id from nodeTypes where name = 'regular'), '7');
insert into nodes(nodeRefId, versionedRefId, nodeType, sysmlId) values(8, 8, (select nodeTypes.id from nodeTypes where name = 'regular'), '8');

insert into edges values(1, 2, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(1, 3, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(2, 5, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(2, 6, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(3, 4, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(7, 2, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(3, 8, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(8, 4, (select edgeTypes.id from edgeTypes where name = 'regular'));

insert into edges values(1, 2, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(2, 6, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(7, 2, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(6, 4, (select edgeTypes.id from edgeTypes where name = 'document'));


*/