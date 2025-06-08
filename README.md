# SnapBid Auction System

SnapBid is a real-time online auction platform built with Spring Boot that allows users to create auctions, place bids, and track their auction activities.

## Features

- User registration and authentication
- Create and manage auction listings
- Real-time bidding functionality
- User dashboard for tracking auctions and bids
- Search and filter auction items

## Prerequisites

To run this application, you'll need:

- Java 17 or higher
- Gradle 7.0 or higher

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/yourusername/SnapBid.git
cd SnapBid
```

### Running the Application

**Method 1: Using the Gradle Wrapper**

```bash
./gradlew bootRun
```

On Windows:

```bash
gradlew.bat bootRun
```

**Method 2: Build and Run the JAR**

```bash
./gradlew build
java -jar build/libs/SnapBid-0.0.1-SNAPSHOT.jar
```

Once started, the application will be available at [http://localhost:8080](http://localhost:8080)
