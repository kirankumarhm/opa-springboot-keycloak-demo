package authz

default allow = false

allow if {
    input.user == "admin"
}

allow if {
    input.user == "alice"
    input.action == "read"
    startswith(input.resource, "document:")
}

allow if {
    input.user == "bob"
    input.action == "read"
    input.resource == "document:1"
}
