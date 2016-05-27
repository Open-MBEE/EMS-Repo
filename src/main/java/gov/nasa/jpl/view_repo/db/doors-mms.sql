\c mms;

create table doors
(
  sysmlId text not null,
  resourceUrl text not null,
  workspaceId text default 'master',
  lastSync timestamp default current_timestamp,
  constraint unique_doors unique(sysmlId, workspaceId, resourceUrl)
);

create index on doors(sysmlId);

create table doorsFields
(
  project text not null,
  propertyId text not null,
  propertyType text default 'string',
  doorsAttr text not null,
  constraint unique_fields unique(project, propertyId, propertyType, doorsAttr)
);
