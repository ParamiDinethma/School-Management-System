Web-Based School Management System
==================================

A comprehensive, web-based platform designed to digitize and streamline the academic and administrative operations of a school.

📚 Project Overview
-------------------

This project is a Java-based web application built to provide a centralized system for managing student data, academic records, attendance, and communication between stakeholders. The primary goal is to replace manual administrative processes with an efficient, secure, and user-friendly digital solution, improving communication and data accessibility for principals, teachers, students, and parents. 


🛠️ Technologies Used
---------------------

This project is built with a modern Java technology stack, focusing on robustness and scalability.

*   **Backend:** Java & Spring Boot
    
*   **Frontend:** Thymeleaf
    
*   **Database:** Microsoft SQL Server (SSMS)
    
*   **Build Tool:** Apache Maven
    
*   **Version Control:** Git & GitHub
    

🚀 Installation & Setup
-----------------------

Follow these steps to get the project running on your local machine.

### **Prerequisites**

*   JDK 17 or higher
    
*   Apache Maven
    
*   Git
    
*   SQL Server Express Edition & SQL Server Management Studio (SSMS)
    

### **1\. Database Setup**

The application requires a SQL Server database named school\_db.

1.  **Create the Database:**
    
    *   Open SSMS and connect to your local server.
        
    *   Right-click the **Databases** folder and select **New Database...**.
        
    *   Name it **wbsms_db** and click **OK**.
        
2.  **Run the SQL Scripts:**
    
    *   Open the schema.sql file in SSMS. Ensure school\_db is selected and execute the script to create all tables.
        
    *   Next, open the data.sql file and execute it to populate the database with initial data (e.g., user roles).
        

### **2\. Application Configuration**
    
1.  **Configure Database Connection:**
    
    *   Navigate to src/main/resources/.
        
    *   Create a copy of application.properties.example and rename it to application.properties.
        
    *   Open the new file and update the spring.datasource.password value with your own SQL Server password.
        

### **3\. Build and Run**

Open the project in your favorite IDE (e.g., IntelliJ IDEA, VS Code) and run it as a Spring Boot application. The server will start on http://localhost:8080.

📖 Usage
--------

The system is designed with role-based access control. Log in with the appropriate credentials to access different features.

*   **Principal / IT Admin:** Has full administrative control. Can manage student and staff records, define courses, create announcements, manage user roles, and generate academic reports.
    
*   **Teacher:** Can mark student attendance, enter grades for examinations, upload learning resources, and communicate with parents.
    
*   **Student:** Can view their class timetable, check attendance history, view exam results, and access learning materials uploaded by teachers.
    
*   **Parent:** Can view their child's academic progress, including attendance records and grades, and communicate directly with teachers.
    
*   **Registrar:** Manages student enrollments and examination schedules.
