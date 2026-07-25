import json

with open('levi-phase-1-2.postman_collection.json', 'r') as f:
    data = json.load(f)

# Create the new folder for Phase 3
phase3_item = {
    "name": "08 Phase 3 (Agile Entities)",
    "item": [
        {
            "name": "1. Boards",
            "item": [
                {
                    "name": "Create Board",
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Create Board returns 200', function () { pm.response.to.have.status(200); });",
                                    "const json = pm.response.json();",
                                    "pm.collectionVariables.set('boardId', json.id);"
                                ]
                            }
                        }
                    ],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"},
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/boards",
                            "host": ["{{baseUrl}}"],
                            "path": ["boards"]
                        },
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"name\": \"Main Board\",\n  \"projectId\": {{projectId}}\n}"
                        }
                    }
                },
                {
                    "name": "Get Boards by Project",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/boards/project/{{projectId}}",
                            "host": ["{{baseUrl}}"],
                            "path": ["boards", "project", "{{projectId}}"]
                        }
                    }
                }
            ]
        },
        {
            "name": "2. Workflow States",
            "item": [
                {
                    "name": "Create Workflow State",
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Create Workflow State returns 200', function () { pm.response.to.have.status(200); });",
                                    "const json = pm.response.json();",
                                    "pm.collectionVariables.set('workflowStateId', json.id);"
                                ]
                            }
                        }
                    ],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"},
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/workflow-states",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflow-states"]
                        },
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"name\": \"To Do\",\n  \"orderIndex\": 1,\n  \"boardId\": {{boardId}}\n}"
                        }
                    }
                },
                {
                    "name": "Get Workflow States by Board",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/workflow-states/board/{{boardId}}",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflow-states", "board", "{{boardId}}"]
                        }
                    }
                }
            ]
        },
        {
            "name": "3. Sprints",
            "item": [
                {
                    "name": "Create Sprint",
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Create Sprint returns 200', function () { pm.response.to.have.status(200); });",
                                    "const json = pm.response.json();",
                                    "pm.collectionVariables.set('sprintId', json.id);"
                                ]
                            }
                        }
                    ],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"},
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/sprints",
                            "host": ["{{baseUrl}}"],
                            "path": ["sprints"]
                        },
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"name\": \"Sprint 1\",\n  \"startDate\": \"2023-10-01T00:00:00\",\n  \"endDate\": \"2023-10-14T23:59:59\",\n  \"projectId\": {{projectId}}\n}"
                        }
                    }
                },
                {
                    "name": "Get Sprints by Project",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Tenant-ID", "value": "{{tenantId}}"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/sprints/project/{{projectId}}",
                            "host": ["{{baseUrl}}"],
                            "path": ["sprints", "project", "{{projectId}}"]
                        }
                    }
                }
            ]
        }
    ]
}

data['item'].append(phase3_item)

# Also rename the collection
data['info']['name'] = "Levi API - Phase 1, 2 & 3 Comprehensive Tests"
data['info']['description'] = "Comprehensive Postman collection for Levi Phase 1, 2, and 3."

with open('levi-phase-1-3.postman_collection.json', 'w') as f:
    json.dump(data, f, indent=2)

print("Updated postman collection created.")
