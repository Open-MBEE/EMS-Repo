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

create table doorsFlags
(
  toggletype text not null,
  flag text not null,
  constraint unique_Flags unique(toggletype)
);

create table doorsfields
(
  project text not null,
  propertyId text not null,
  propertyType text default 'string',
  doorsAttr text not null,
  constraint unique_fields unique(project, propertyId, propertyType, doorsAttr)
);

create table doorsartifactmappings 
(
  project text not null, 
  doorsartifacttype text not null, 
  sysmlappliedmetatype text not null
);

create table doorsprojectmappings 
(
  sysmlprojectid text not null, 
  doorsproject text not null
);

create table doorsartifactlinkmappings 
(
  project text not null, 
  sysmlappliedmetatypeid text not null, 
  uri text not null
);

create table doorsprojectfoldermappings
(
  project text not null,
  doorsfolderid text not null,
  constraint unique_folders unique(project)
);
